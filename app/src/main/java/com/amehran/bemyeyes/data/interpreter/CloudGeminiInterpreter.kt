package com.amehran.bemyeyes.data.interpreter

import android.graphics.Bitmap
import android.util.Base64
import com.amehran.bemyeyes.domain.interpreter.SceneInterpreter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class CloudGeminiInterpreter @Inject constructor() : SceneInterpreter {

    // Securely retrieved from local.properties via BuildConfig
    private val apiKey = com.amehran.bemyeyes.BuildConfig.GEMINI_API_KEY 

    override suspend fun describe(bitmap: Bitmap, languageCode: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "Error: API Key is missing. Check local.properties."
        }
        
        val promptText = if (languageCode.lowercase() == "fa") {
             "این صحنه را برای یک فرد نابینا توصیف کنید. کوتاه باشد." // Describe this scene for a blind person. Keep it concise.
        } else {
             "Describe this scene for a blind person. Concisely."
        }
        
        // 0. DIAGNOSTIC: Check Inputs
        android.util.Log.e("GeminiInterpreter", "Starting describe(). Language: $languageCode, Model Check...")

        // 0. DIAGNOSTIC: List Available Models
        var determinedModel: String? = null
        try {
            // ... (keep existing model listing code, but allow it to be quiet unless error)
            // For brevity in diff, ensuring we log the RESULT of this check with Log.e
            val listUrl = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
             // ... connection setup ...
            val listConnection = listUrl.openConnection() as HttpURLConnection
            listConnection.requestMethod = "GET"
            listConnection.connectTimeout = 5000
            
            if (listConnection.responseCode == 200) {
                 // ... parse ...
                 val reader = BufferedReader(InputStreamReader(listConnection.inputStream))
                 val response = StringBuilder()
                 var line: String?
                 while (reader.readLine().also { line = it } != null) response.append(line)
                 reader.close()
                 
                 val json = JSONObject(response.toString())
                 val models = json.optJSONArray("models")
                 val availableNames = mutableListOf<String>()
                 if (models != null) {
                     for (i in 0 until models.length()) {
                         val m = models.getJSONObject(i)
                         val name = m.optString("name")
                         val methods = m.optJSONArray("supportedGenerationMethods")
                         var supportsGenerate = false
                         if (methods != null) {
                             for (j in 0 until methods.length()) {
                                 if (methods.getString(j) == "generateContent") supportsGenerate = true
                             }
                         }
                         if (supportsGenerate) availableNames.add(name.replace("models/", ""))
                     }
                 }
                 
                 determinedModel = availableNames.firstOrNull { it.contains("flash") && it.contains("1.5") }
                    ?: availableNames.firstOrNull { it.contains("flash") }
                    ?: availableNames.firstOrNull { it.contains("pro") && it.contains("vision") }
                    ?: availableNames.firstOrNull { it.contains("pro") }
                    ?: availableNames.firstOrNull()
                 
                 android.util.Log.e("GeminiInterpreter", "Selected Model: $determinedModel (from ${availableNames.size} available)")
            } else {
                 android.util.Log.e("GeminiInterpreter", "ListModels failed: ${listConnection.responseCode}")
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiInterpreter", "Failed to list models", e)
        }
        
        // ... (keep image prep) ...
        // 1. Prepare Image (Base64) - Optimized for Bandwidth
        val base64Image = try {
            // Resize if too large (e.g. > 800px width) to save bandwidth and prevent timeouts
            val maxDimension = 800
            var finalBitmap = bitmap
            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                finalBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream) // 70% quality is sufficient for AI
            val imageBytes = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("GeminiInterpreter", "Error resizing/encoding image", e)
            return@withContext "Error processing image."
        }

        // 2. Construct JSON Payload
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", promptText))
                    put(JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                })
            }))
        }
        val jsonString = jsonBody.toString().toByteArray()

        // 3. Select Model
        // Force fallback list to always include standard free-tier friendly models AND new experimental ones
        val safeModels = listOf(
            "gemini-2.0-flash-exp", 
            "gemini-1.5-flash-002",
            "gemini-1.5-flash",
            "gemini-1.5-flash-8b", 
            "gemini-1.5-flash-latest",
            "gemini-pro-vision",
            "gemini-1.0-pro-vision-latest"
        )
        val modelsToTry = if (determinedModel != null) {
            // Try the detected one first, but fallback to known safe ones if it fails
            listOf(determinedModel) + safeModels.filter { it != determinedModel }
        } else {
            safeModels + listOf("gemini-pro-vision")
        }

        var friendlyErrorMessage: String? = null
        val lastError = StringBuilder()

        for (model in modelsToTry) {
            android.util.Log.e("GeminiInterpreter", "Attempting generation with: $model")
            try {
                val cleanModelName = model!!.replace("models/", "")
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$cleanModelName:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000 

                connection.outputStream.use { os ->
                    os.write(jsonString)
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                     val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    val jsonResponse = JSONObject(response.toString())
                    val candidates = jsonResponse.optJSONArray("candidates")
                    val textParts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                    val text = textParts?.optJSONObject(0)?.optString("text")

                    android.util.Log.e("GeminiInterpreter", "SUCCESS! Response: ${text?.take(50)}...")
                    return@withContext text ?: "No description received."
                } else {
                    val reader = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream))
                    val errorResponse = StringBuilder()
                    var line: String? 
                    while (reader.readLine().also { line = it } != null) errorResponse.append(line)
                    
                    val errorStr = errorResponse.toString()
                    android.util.Log.e("GeminiInterpreter", "Model $model failed ($responseCode): $errorStr")
                    
                    if (responseCode == 429) {
                        // Check if it's a Rate Limit (short wait) or Daily Quota (long wait)
                        val isRateLimit = errorStr.contains("retryDelay") || errorStr.contains("default: 60s")
                        
                        friendlyErrorMessage = if (isRateLimit) {
                            if (languageCode.lowercase() == "fa") "سرعت درخواست‌ها زیاد است. لطفاً یک دقیقه صبر کنید."
                            else "Too many requests. Please wait a minute."
                        } else {
                            if (languageCode.lowercase() == "fa") "سهمیه روزانه تمام شده است. فردا امتحان کنید."
                            else "Daily quota exceeded today."
                        }
                    }
                    
                    lastError.append("$model ($responseCode), ")
                }
            } catch (e: Exception) {
                android.util.Log.e("GeminiInterpreter", "Exception with $model", e)
                lastError.append("$model (${e.message}), ")
            }
        }

        return@withContext friendlyErrorMessage ?: "Error: Failed to connect. ($lastError)"
    }
}
