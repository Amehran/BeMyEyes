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
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setup() {
        viewModel = CameraViewModel(objectDetector, textToSpeechManager)
    }

    @Test
    fun `when object is detected, it should update flow and speak the label`() = runBlocking {
        // Given
        val bitmap: Bitmap = mockk()
        val expectedDetections = listOf(Detection("Person", 0.9f))
        coEvery { objectDetector.detect(bitmap) } returns flowOf(expectedDetections)

        // When
        viewModel.detect(bitmap)

        // Then
        viewModel.detections.test { // Using Turbine for robust flow testing
            val actualDetections = awaitItem()
            assertEquals(expectedDetections, actualDetections)

            // Verify that the speak method was called with the correct label
            verify { textToSpeechManager.speak("Person") }

            cancelAndIgnoreRemainingEvents()
        }
    }
}
