package com.amehran.bemyeyes.domain.model

data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: android.graphics.RectF,
    val distanceMeters: Float? = null
) {
    fun getDescription(): String {
        // Model input/box is normalized 0..1
        val centerX = boundingBox.centerX()
        val position = when {
            centerX < 0.35f -> "to the left"
            centerX > 0.65f -> "to the right"
            else -> "in front"
        }

        val heightRatio = boundingBox.height() // 0..1
        val distance = if (distanceMeters != null) {
             when {
                 distanceMeters < 2.0 -> "very close"
                 distanceMeters < 5.0 -> "close"
                 else -> "at %.1fm".format(distanceMeters)
             }
        } else {         
            when {
                heightRatio > 0.6 -> "very close"
                heightRatio > 0.3 -> "close"
                else -> ""
            }
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

