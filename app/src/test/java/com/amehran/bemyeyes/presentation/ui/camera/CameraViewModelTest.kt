package com.amehran.bemyeyes.presentation.ui.camera

import android.graphics.Bitmap
import app.cash.turbine.test
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import com.amehran.bemyeyes.domain.repository.TextToSpeechManager
import com.amehran.bemyeyes.domain.repository.VibrationManager
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
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class CameraViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val objectDetector: ObjectDetector = mockk(relaxed = true)
    private val textToSpeechManager: TextToSpeechManager = mockk(relaxed = true)
    private val vibrationManager: VibrationManager = mockk(relaxed = true)
    private val speechManager: com.amehran.bemyeyes.domain.repository.SpeechManager = mockk(relaxed = true)
    private val detectionTracker: com.amehran.bemyeyes.domain.tracker.DetectionTracker = mockk(relaxed = true)
    private val sceneDescriber: com.amehran.bemyeyes.domain.describer.SceneDescriber = mockk(relaxed = true)
    private val backendRepository: com.amehran.bemyeyes.domain.repository.BackendRepository = mockk(relaxed = true)
    private val deviceInterpreter: com.amehran.bemyeyes.data.interpreter.OnDeviceGeminiInterpreter = mockk(relaxed = true)
    private lateinit var viewModel: CameraViewModel

    @Before
    fun setup() {
        // By default, the mocked tracker just returns whatever it gets as "all stable"
        // and treats EVERYTHING as "newly stable" initially to pass the "should speak" tests.
        io.mockk.every { detectionTracker.process(any()) } answers { 
            val input = firstArg<List<Detection>>()
            com.amehran.bemyeyes.domain.tracker.DetectionTracker.TrackingResult(
                allStableDetections = input,
                newStableDetections = input // Allow test cases to trigger speech immediately
            )
        }
        
        // Default Mock Describer
        io.mockk.every { sceneDescriber.describe(any()) } answers {
            val list = firstArg<List<Detection>>()
            list.joinToString(" and ") { it.label } + " ahead"
        }
        
        viewModel = CameraViewModel(
            objectDetector,
            textToSpeechManager,
            vibrationManager,
            speechManager,
            detectionTracker,
            sceneDescriber,
            backendRepository,
            deviceInterpreter
        )
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
            // New Phase 5 Logic: Speak Scene
            verify { textToSpeechManager.speak("Person ahead") }

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
        val carDetection = Detection("car", 0.95f, rectFar) // Far, Urgent, and High Confidence

        val detections = listOf(personDetection, carDetection)
        coEvery { objectDetector.detect(bitmap) } returns detections

        // When
        viewModel.detect(bitmap)

        // Then
        // Should speak the WHOLE SCENE
        // Our mock describer joins with " and ".
        // Note: The order depends on how `allStableDetections` is passed.
        // `objectDetector` returns a list. `DetectionTracker` passes it through.
        // `speakScene` passes it to `describer`.
        // `describer` implementation (real one) sorts.
        // Our MOCK describer just joins them in input order.
        // Detection List order: [Person, Car].
        // Expect: "Person and car ahead"
        
        verify { textToSpeechManager.speak("Person and car ahead") }
        
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

    @Test
    fun `should prevent repetitive audio for same object even if interleaved with others`() = runBlocking {
        // This test simulates the "Flip-Flop" bug where seeing B resets the timer for A.
        
        // Setup Mocks
        val bitmap: Bitmap = mockk()
        val rect = mockk<android.graphics.RectF>()
        io.mockk.every { rect.centerX() } returns 160f
        io.mockk.every { rect.height() } returns 160f
        
        val chair = Detection("chair", 0.9f, rect)
        val table = Detection("table", 0.9f, rect)

        // 1. Detect CHAIR
        coEvery { objectDetector.detect(any<Bitmap>()) } returns listOf(chair)
        viewModel.detect(bitmap)
        verify(exactly = 1) { textToSpeechManager.speak(match { it.contains("chair") }) }

        // 2. Detect TABLE
        coEvery { objectDetector.detect(any<Bitmap>()) } returns listOf(table)
        viewModel.detect(bitmap)
        verify(exactly = 1) { textToSpeechManager.speak(match { it.contains("table") }) }

        // 3. Detect CHAIR again (Immediately)
        // DESIRED BEHAVIOR: Silence (Cooldown hasn't passed for Chair)
        // CURRENT BUG: Speak (Because "Chair" != "Table")
        coEvery { objectDetector.detect(any<Bitmap>()) } returns listOf(chair)
        
        // IMPORTANT: For this test step, we must simulate that "Chair" is NOT "newly stable",
        // otherwise Rule 1 (Always speak new stable) will trigger and bypass the debounce.
        io.mockk.every { detectionTracker.process(any()) } returns com.amehran.bemyeyes.domain.tracker.DetectionTracker.TrackingResult(
            allStableDetections = listOf(chair),
            newStableDetections = emptyList() // It's not new, it's just persistent
        )

        viewModel.detect(bitmap)
        
        // We expect verify call count for "chair" to remain 1, NOT 2
        verify(exactly = 1) { textToSpeechManager.speak(match { it.contains("chair") }) }
    }
}
