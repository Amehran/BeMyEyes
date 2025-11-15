package com.amehran.bemyeyes.data.repository

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.amehran.bemyeyes.domain.model.Detection
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class TfLiteObjectDetectorTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var objectDetector: TfLiteObjectDetector

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        objectDetector = TfLiteObjectDetector(context)
    }

    @Test
    fun testObjectDetection_doesNotCrashAndEmitsDetections() = runBlocking {
        // Given a dummy Bitmap
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)

        // When calling detect
        val detectionsFlow = objectDetector.detect(bitmap)
        val detections = detectionsFlow.first() // This will throw if the flow is empty

        // Then assert that the flow emitted a list (it can be empty, but not null or error)
        assertNotNull(detections)
    }
}
