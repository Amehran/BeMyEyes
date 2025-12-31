package com.amehran.bemyeyes.data.repository

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.amehran.bemyeyes.domain.repository.VibrationManager

class SystemVibrationManager(private val context: Context) : VibrationManager {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun vibrateCaution() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Double pulse for caution
            val timing = longArrayOf(0, 100, 100, 100)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timing, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            // Double pulse pattern: wait 0, vibrate 100, wait 100, vibrate 100
            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
        }
    }

    override fun vibrateClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
}
