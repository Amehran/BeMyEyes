package com.amehran.bemyeyes.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    isCurtainMode: Boolean,
    onCurtainModeChange: (Boolean) -> Unit,
    isCloudMode: Boolean,
    onCloudModeChange: (Boolean) -> Unit,
    isFarsi: Boolean,
    onLanguageChange: (Boolean) -> Unit,
    isRealtimeDetectionEnabled: Boolean,
    onRealtimeDetectionChange: (Boolean) -> Unit,
    isPowerSaverMode: Boolean,
    onPowerSaverChange: (Boolean) -> Unit
) {
    androidx.activity.compose.BackHandler {
        onDismiss()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Curtain Mode Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Curtain Mode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Black screen for privacy & battery",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = isCurtainMode,
                    onCheckedChange = onCurtainModeChange
                )
            }

            HorizontalDivider()
            
            // Power Saver Mode Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Power Saver",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Lowers resolution to save battery",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = isPowerSaverMode,
                    onCheckedChange = onPowerSaverChange
                )
            }

            HorizontalDivider()

            // AI Model Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Model",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isCloudMode) "Cloud (More Intelligent)" else "Local (More Private)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = isCloudMode,
                    onCheckedChange = onCloudModeChange,
                    thumbContent = {
                        // Optional: Icons on thumb
                    }
                )
            }

            HorizontalDivider()

            // Language Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Language / زبان",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isFarsi) "Farsi (Persian)" else "English",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = isFarsi,
                    onCheckedChange = onLanguageChange
                )
            }
            
            HorizontalDivider()

            // Realtime Detection Setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Realtime Detection",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Automatically detect objects in camera view",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = isRealtimeDetectionEnabled,
                    onCheckedChange = onRealtimeDetectionChange
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}
