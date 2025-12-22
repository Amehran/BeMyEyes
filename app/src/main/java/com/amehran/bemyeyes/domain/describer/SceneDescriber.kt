package com.amehran.bemyeyes.domain.describer

import com.amehran.bemyeyes.domain.model.Detection
import javax.inject.Inject

class SceneDescriber @Inject constructor() {

    fun describe(detections: List<Detection>): String {
        if (detections.isEmpty()) return ""

        val urgentLabels = setOf("car", "bus", "truck", "traffic light", "stop sign", "fire hydrant")

        // Sort: Urgent first, then by confidence
        val sortedDetections = detections.sortedWith(
            compareByDescending<Detection> { urgentLabels.contains(it.label) }
                .thenByDescending { it.confidence }
        )

        val labels = sortedDetections.map { it.label }
        
        return if (labels.size == 1) {
            "${labels[0]} ahead"
        } else {
            val allButLast = labels.dropLast(1).joinToString(", ")
            val last = labels.last()
            "$allButLast and $last ahead"
        }
    }
}
