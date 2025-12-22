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

    data class TrackingResult(
        val allStableDetections: List<Detection>,
        val newStableDetections: List<Detection> // "Freshly" stable this frame
    )

    fun process(detections: List<Detection>): TrackingResult {
        // Map current detections by label for easy lookup
        val inputMap = detections.associateBy { it.label }
        val newlyStable = mutableListOf<Detection>()

        // 1. Update existing trackers
        val iterator = trackedObjects.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val label = entry.key
            val tracker = entry.value
            val wasStableBefore = tracker.isStable()

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
                if (!wasStableBefore) {
                    tracker.seenCount = 0
                }
            }

            // Check if it JUST became stable this frame
            if (!wasStableBefore && tracker.isStable()) {
                newlyStable.add(tracker.detection)
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

        // 3. Return results
        val allStable = trackedObjects.values
            .filter { it.isStable() }
            .map { it.detection }
            .toList()

        return TrackingResult(allStable, newlyStable)
    }
}
