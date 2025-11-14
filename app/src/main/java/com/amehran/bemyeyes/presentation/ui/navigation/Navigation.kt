package com.amehran.bemyeyes.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amehran.bemyeyes.presentation.ui.camera.CameraScreen
import com.amehran.bemyeyes.presentation.ui.splash.SplashScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(navController = navController)
        }
        composable("camera") {
            CameraScreen()
        }
    }
}
