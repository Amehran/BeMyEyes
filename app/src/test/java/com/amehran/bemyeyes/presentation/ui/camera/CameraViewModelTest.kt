package com.amehran.bemyeyes.presentation.ui.camera

import android.graphics.Bitmap
import app.cash.turbine.test
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import com.amehran.bemyeyes.domain.repository.TextToSpeechManager
import com.amehran.bemyeyes.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CameraViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val objectDetector: ObjectDetector = mockk(relaxed = true)
    private val textToSpeechManager: TextToSpeechManager = mockk(relaxed = true)
    private val vibrationManager: com.amehran.bemyeyes.domain.repository.VibrationManager = mockk(relaxed = true)
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setup() {
        viewModel = CameraViewModel(objectDetector, textToSpeechManager, vibrationManager)
    }

    @Test
    fun `when object is detected, it should update flow and speak the description`() = runBlocking {
        // Given
        val bitmap: Bitmap = mockk()
        val mockRect = mockk<android.graphics.RectF>()
        io.mockk.every { mockRect.centerX() } returns 160f
        io.mockk.every { mockRect.height() } returns 120f
        
        val expectedDetections = listOf(Detection("Person", 0.9f, mockRect))
        coEvery { objectDetector.detect(bitmap) } returns expectedDetections

        // When
        viewModel.detect(bitmap)

        // Then
        viewModel.detections.test {
            val actualDetections = awaitItem()
            assertEquals(expectedDetections, actualDetections)

            // Verify that the speak method was called with the correct description
            verify { textToSpeechManager.speak("Person, in front, close") }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when urgent object is detected, it should be prioritized`() = runBlocking {
        // Given
        val bitmap: Bitmap = mockk()
        val rectFar = mockk<android.graphics.RectF>()
        io.mockk.every { rectFar.centerX() } returns 160f
        io.mockk.every { rectFar.height() } returns 50f // Far

        val rectClose = mockk<android.graphics.RectF>()
        io.mockk.every { rectClose.centerX() } returns 160f
        io.mockk.every { rectClose.height() } returns 200f // Very Close

        val personDetection = Detection("Person", 0.9f, rectClose) // Close but not urgent
        val carDetection = Detection("car", 0.8f, rectFar) // Far but urgent

        val detections = listOf(personDetection, carDetection)
        coEvery { objectDetector.detect(bitmap) } returns detections

        // When
        viewModel.detect(bitmap)

        // Then
        // Should speak the Car because it is Urgent, even though Person is closer/more confident
        verify { textToSpeechManager.speak("car, in front") }
        
        // Should also vibrate for urgent object
        verify { vibrationManager.vibrateCaution() }
    }

    @Test
    fun `when no object is detected, it should update flow but not speak`() = runBlocking {
        // Given
        val bitmap: Bitmap = mockk()
        val expectedDetections = emptyList<Detection>()
        coEvery { objectDetector.detect(bitmap) } returns expectedDetections

        // When
        viewModel.detect(bitmap)

        // Then
        viewModel.detections.test {
            val actualDetections = awaitItem()
            assertEquals(expectedDetections, actualDetections)

            // Verify that speak was NOT called
            verify(exactly = 0) { textToSpeechManager.speak(any()) }

            cancelAndIgnoreRemainingEvents()
        }
    }
}
