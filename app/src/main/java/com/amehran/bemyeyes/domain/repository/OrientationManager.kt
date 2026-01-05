package com.amehran.bemyeyes.domain.repository

import kotlinx.coroutines.flow.StateFlow

data class OrientationData(
    val azimuth: Float = 0f, // 0-360 degrees (Compass Heading)
    val pitch: Float = 0f    // -90 to +90 degrees (Tilt)
)

interface OrientationManager {
    val orientation: StateFlow<OrientationData>
    fun start()
    fun stop()
}
