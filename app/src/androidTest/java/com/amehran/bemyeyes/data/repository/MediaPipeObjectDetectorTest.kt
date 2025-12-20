package com.amehran.bemyeyes.data.repository

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MediaPipeObjectDetectorTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var objectDetector: MediaPipeObjectDetector

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // This will fail to compile until MediaPipeObjectDetector is created
        objectDetector = MediaPipeObjectDetector(context)
    }

    @Test
    fun testObjectDetection_initializesAnd_runsInference() = runBlocking {
        // Given a blank Bitmap (MediaPipe expects ARGB_8888)
        val bitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)

        // When calling detect
        val detections = objectDetector.detect(bitmap)

        // Then assert we got a list back (likely empty for black image)
        assertNotNull(detections)
        // Ensure no crash occurred
    }
}
