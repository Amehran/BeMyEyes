package com.amehran.bemyeyes.presentation.ui.navigation

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.rule.GrantPermissionRule
import com.amehran.bemyeyes.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // This rule automatically grants the camera permission BEFORE the test runs.
    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    // The compose rule must run AFTER the permissions are granted.
    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun splashScreen_isDisplayed_onAppStart() {
        composeTestRule.onNodeWithText("Be My Eyes").assertIsDisplayed()
    }

    @Test
    fun cameraScreen_isDisplayed_afterSplashScreen() {
        // This test launches the full app. Because the permission is already granted,
        // the camera preview will display immediately after the splash screen.
        composeTestRule.waitUntil(timeoutMillis = 5000) { // Increased timeout for safety
            composeTestRule
                .onAllNodesWithTag("camera_preview")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Now, assert that the camera preview is actually displayed.
        composeTestRule.onNodeWithTag("camera_preview").assertIsDisplayed()
    }
}