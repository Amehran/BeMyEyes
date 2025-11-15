package com.amehran.bemyeyes.presentation.ui.camera

import android.graphics.Bitmap
import app.cash.turbine.test
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import com.amehran.bemyeyes.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
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

    private val objectDetector: ObjectDetector = mockk()
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setup() {
        viewModel = CameraViewModel(objectDetector)
    }

    @Test
    fun `when detect is called, it should update the detections flow`() = runBlocking {
        // Given
        val bitmap: Bitmap = mockk()
        val expectedDetections = listOf(Detection("Person", 0.9f), Detection("Car", 0.8f))
        coEvery { objectDetector.detect(bitmap) } returns flowOf(expectedDetections)

        // When
        viewModel.detect(bitmap)

        // Then
        viewModel.detections.test { // Using Turbine for robust flow testing
            val actualDetections = awaitItem()
            assertEquals(expectedDetections, actualDetections)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
