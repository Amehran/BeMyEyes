package com.amehran.bemyeyes.presentation.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
    onPowerSaverChange: (Boolean) -> Unit,
    isOutdoorMode: Boolean,
    onOutdoorModeChange: (Boolean) -> Unit,
    isTtsEnabled: Boolean,
    onTtsChange: (Boolean) -> Unit
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

            // Enable App Voice (TTS)
            SettingsSwitchRow(
                title = "Enable App Voice (TTS)",
                subtitle = if (isTtsEnabled) "App speaks descriptions" else "Silent (For Screen Reader Users)",
                checked = isTtsEnabled,
                onCheckedChange = onTtsChange
            )
            HorizontalDivider()

            // Curtain Mode Setting
            SettingsSwitchRow(
                title = "Curtain Mode",
                subtitle = "Black screen for privacy & battery",
                checked = isCurtainMode,
                onCheckedChange = onCurtainModeChange
            )
            HorizontalDivider()
            
            // Environment Setting
            SettingsSwitchRow(
                title = "Environment",
                subtitle = if (isOutdoorMode) "Outdoor (Navigation Focus)" else "Indoor (Object Focus)",
                checked = isOutdoorMode,
                onCheckedChange = onOutdoorModeChange
            )
            HorizontalDivider()
            
            // Power Saver Mode Setting
            SettingsSwitchRow(
                title = "Power Saver",
                subtitle = "Lowers resolution to save battery",
                checked = isPowerSaverMode,
                onCheckedChange = onPowerSaverChange
            )
            HorizontalDivider()

            // AI Model Setting
            SettingsSwitchRow(
                title = "AI Model",
                subtitle = if (isCloudMode) "Cloud (More Intelligent)" else "Local (More Private)",
                checked = isCloudMode,
                onCheckedChange = onCloudModeChange
            )
            HorizontalDivider()

            // Language Setting
            SettingsSwitchRow(
                title = "Language / زبان",
                subtitle = if (isFarsi) "Farsi (Persian)" else "English",
                checked = isFarsi,
                onCheckedChange = onLanguageChange
            )
            HorizontalDivider()

            // Realtime Detection Setting
            SettingsSwitchRow(
                title = "Realtime Detection",
                subtitle = "Automatically detect objects in camera view",
                checked = isRealtimeDetectionEnabled,
                onCheckedChange = onRealtimeDetectionChange
            )
            
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

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null // Handled by toggleable modifier on Row
        )
    }
}
