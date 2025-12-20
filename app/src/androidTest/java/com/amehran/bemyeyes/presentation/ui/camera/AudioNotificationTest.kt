package com.amehran.bemyeyes.presentation.ui.camera

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.rule.GrantPermissionRule
import com.amehran.bemyeyes.MainActivity
import com.amehran.bemyeyes.domain.model.Detection
import com.amehran.bemyeyes.domain.repository.ObjectDetector
import com.amehran.bemyeyes.domain.repository.TextToSpeechManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class AudioNotificationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var objectDetector: ObjectDetector // This will be a mock provided by TestAppModule

    @Inject
    lateinit var textToSpeechManager: TextToSpeechManager // This will be a mock provided by TestAppModule

    @Test
    fun whenObjectIsDetected_thenSpeakIsCalledWithCorrectLabel() {
        // Given the detector will find a cat
        val mockBox = android.graphics.RectF(0f, 0f, 100f, 100f)
        coEvery { objectDetector.detect(any()) } returns listOf(Detection("cat", 0.95f, mockBox))

        // When the camera screen is displayed
        composeTestRule.waitForIdle() // Wait for UI to settle and analysis to run

        // Then verify that the TextToSpeechManager was told to speak "cat"
        coVerify(timeout = 2000) { // Verify with a timeout to allow for detection to happen
            textToSpeechManager.speak("cat")
        }
    }
}
