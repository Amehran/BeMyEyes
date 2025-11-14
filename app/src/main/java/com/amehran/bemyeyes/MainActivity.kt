package com.amehran.bemyeyes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amehran.bemyeyes.presentation.ui.navigation.Navigation
import com.amehran.bemyeyes.ui.theme.BeMyEyesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeMyEyesTheme {
                Navigation()
            }
        }
    }
}
