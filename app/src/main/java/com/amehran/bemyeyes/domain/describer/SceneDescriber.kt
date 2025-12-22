package com.amehran.bemyeyes.domain.describer

import com.amehran.bemyeyes.domain.model.Detection
import javax.inject.Inject

class SceneDescriber @Inject constructor() {

    fun describe(detections: List<Detection>): String {
        if (detections.isEmpty()) return ""

        val urgentLabels = setOf("car", "bus", "truck", "traffic light", "stop sign", "fire hydrant")

        // Group by label first to count them
        val counts = detections.groupingBy { it.label }.eachCount()

        // Create a list of "Label" or "N Labels" strings
        // We still want to sort the GROUPS by urgency/importance.
        // We'll use the "most confident" detection of that group to decide sort order, or just label urgency.
        val groupedDescriptions = counts.map { (label, count) ->
            val description = if (count > 1) "$count ${pluralize(label)}" else label
            val isUrgent = urgentLabels.contains(label)
            val maxConfidence = detections.filter { it.label == label }.maxOf { it.confidence }
            
            Triple(description, isUrgent, maxConfidence)
        }.sortedWith(
            compareByDescending<Triple<String, Boolean, Float>> { it.second } // Urgent?
                .thenByDescending { it.third } // Confidence
        ).map { it.first }

        return if (groupedDescriptions.size == 1) {
            "${groupedDescriptions[0]} ahead"
        } else {
            val allButLast = groupedDescriptions.dropLast(1).joinToString(", ")
            val last = groupedDescriptions.last()
            "$allButLast and $last ahead"
        }
    }

    private fun pluralize(label: String): String {
        return if (label.endsWith("s")) "${label}es" else "${label}s"
    }
}
