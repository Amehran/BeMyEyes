package com.amehran.bemyeyes.domain.tracker

import com.amehran.bemyeyes.domain.model.Detection

import javax.inject.Inject

class DetectionTracker @Inject constructor() {

    private val trackedObjects = mutableMapOf<String, TrackedObject>()
    private val STABILITY_THRESHOLD = 3
    private val MAX_MISSING_FRAMES = 5

    private data class TrackedObject(
        var detection: Detection,
        var seenCount: Int = 0,
        var missingCount: Int = 0
    ) {
        fun isStable() = seenCount >= 3
    }

    fun process(detections: List<Detection>): List<Detection> {
        // Map current detections by label for easy lookup
        // Note: This simplistic approach picks the last detection if multiple exist with same label.
        // For Phase 4 (Stability), this is acceptable.
        val inputMap = detections.associateBy { it.label }

        // 1. Update existing trackers
        val iterator = trackedObjects.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val label = entry.key
            val tracker = entry.value

            val newDetection = inputMap[label]

            if (newDetection != null) {
                // Object matched
                tracker.seenCount++
                tracker.missingCount = 0
                tracker.detection = newDetection
            } else {
                // Object missing
                tracker.missingCount++
                
                // If it wasn't stable yet, a miss resets the progress
                if (!tracker.isStable()) {
                    tracker.seenCount = 0
                }
            }

            // Prune dead objects
            if (tracker.missingCount >= MAX_MISSING_FRAMES) {
                iterator.remove()
            }
        }

        // 2. Add new objects that aren't being tracked yet
        for (detection in detections) {
            if (!trackedObjects.containsKey(detection.label)) {
                trackedObjects[detection.label] = TrackedObject(
                    detection = detection,
                    seenCount = 1,
                    missingCount = 0
                )
            }
        }

        // 3. Return only stable objects
        return trackedObjects.values
            .filter { it.isStable() }
            .map { it.detection }
            .toList()
    }
}
