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
            val groupDetections = detections.filter { it.label == label }
            val minDistance = groupDetections.mapNotNull { it.distanceMeters }.minOrNull()
            
            val baseLabel = if (count > 1) "$count ${pluralize(label)}" else label
            
            val description = if (minDistance != null) {
                // Round to 1 decimal place or integer for clarity
                val distStr = "%.1f".format(minDistance)
                if (count > 1) "$baseLabel, closest at $distStr meters" else "$baseLabel at $distStr meters"
            } else {
                baseLabel
            }

            val isUrgent = urgentLabels.contains(label)
            val maxConfidence = groupDetections.maxOf { it.confidence }
            
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
