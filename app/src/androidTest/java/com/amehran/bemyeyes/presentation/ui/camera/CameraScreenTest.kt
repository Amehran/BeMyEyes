package com.amehran.bemyeyes.presentation.ui.camera

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.amehran.bemyeyes.ui.theme.BeMyEyesTheme
import org.junit.Rule
import org.junit.Test

class CameraScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cameraScreen_displaysCameraPreview() {
        // Given
        composeTestRule.setContent {
            BeMyEyesTheme {
                CameraScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithTag("camera_preview").assertIsDisplayed()
    }
}
