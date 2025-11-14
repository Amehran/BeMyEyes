package com.amehran.bemyeyes.presentation.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.amehran.bemyeyes.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun splashScreen_isDisplayed_onAppStart() {
        composeTestRule.onNodeWithText("Be My Eyes").assertIsDisplayed()
    }

    @Test
    fun cameraScreen_isDisplayed_afterSplashScreen() {
        // Wait until the camera preview is visible
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithTag("camera_preview").fetchSemanticsNodes().isNotEmpty()
        }

        // Assert that the camera preview is displayed
        composeTestRule.onNodeWithTag("camera_preview").assertIsDisplayed()
    }
}
