package com.amehran.bemyeyes.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.amehran.bemyeyes.domain.repository.OrientationData
import com.amehran.bemyeyes.domain.repository.OrientationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.toDegrees

@Singleton
class OrientationManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : OrientationManager, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val _orientation = MutableStateFlow(OrientationData())
    override val orientation: StateFlow<OrientationData> = _orientation.asStateFlow()

    private var rotationVectorSensor: Sensor? = null
    private var isListening = false

    init {
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    override fun start() {
        if (isListening || rotationVectorSensor == null) return
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        isListening = true
    }

    override fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Azimuth (heading) is orientationAngles[0]
            // Pitch is orientationAngles[1]
            // Roll is orientationAngles[2]

            var azimuthDeg = toDegrees(orientationAngles[0].toDouble()).toFloat()
            if (azimuthDeg < 0) {
                azimuthDeg += 360f
            }

            val pitchDeg = toDegrees(orientationAngles[1].toDouble()).toFloat()

            _orientation.value = OrientationData(
                azimuth = azimuthDeg,
                pitch = pitchDeg
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
