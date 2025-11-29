package com.amehran.bemyeyes.domain.model

data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: android.graphics.RectF
) {
    fun getDescription(): String {
        // Model input size is 320x320
        val imageWidth = 320f
        val imageHeight = 320f

        val centerX = boundingBox.centerX()
        val position = when {
            centerX < imageWidth * 0.35 -> "to the left"
            centerX > imageWidth * 0.65 -> "to the right"
            else -> "in front"
        }

        val heightRatio = boundingBox.height() / imageHeight
        val distance = when {
            heightRatio > 0.6 -> "very close"
            heightRatio > 0.3 -> "close"
            else -> ""
        }

        // Urgent objects that should be announced with more urgency or specific phrasing
        val urgentLabels = setOf("car", "bus", "truck", "traffic light", "stop sign", "fire hydrant")
        
        val isUrgent = urgentLabels.contains(label)
        val prefix = if (isUrgent && distance.isNotEmpty()) "Caution: " else ""

        return if (distance.isNotEmpty()) {
            "$prefix$label, $position, $distance"
        } else {
            "$prefix$label, $position"
        }
    }
}

