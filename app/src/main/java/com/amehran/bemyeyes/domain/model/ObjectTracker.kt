package com.amehran.bemyeyes.domain.model

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

data class TrackedObject(
    val id: Int,
    val label: String,
    var boundingBox: RectF,
    var confidence: Float,
    var lastSeenTime: Long,
    var firstSeenTime: Long,
    var lastSpokenTime: Long = 0,
    var consecutiveFrames: Int = 1
)

class ObjectTracker {
    private val trackedObjects = mutableListOf<TrackedObject>()
    private var nextId = 0
    private val maxDisappearedTimeMs = 1000L // Keep object in memory for 1s if lost
    private val iouThreshold = 0.5f

    fun processDetections(detections: List<Detection>): List<TrackedObject> {
        val currentTime = System.currentTimeMillis()
        val currentDetections = detections.toMutableList()
        val matchedObjects = mutableListOf<TrackedObject>()

        // 1. Match existing tracked objects to new detections
        val it = trackedObjects.iterator()
        while (it.hasNext()) {
            val tracked = it.next()
            
            // Find best matching detection
            var bestMatch: Detection? = null
            var bestIoU = 0f
            
            val detectionIterator = currentDetections.iterator()
            while (detectionIterator.hasNext()) {
                val detection = detectionIterator.next()
                if (detection.label == tracked.label) { // Only match same label
                    val iou = calculateIoU(tracked.boundingBox, detection.boundingBox)
                    if (iou > iouThreshold && iou > bestIoU) {
                        bestIoU = iou
                        bestMatch = detection
                    }
                }
            }

            if (bestMatch != null) {
                // Update tracked object
                tracked.boundingBox = bestMatch.boundingBox
                tracked.confidence = bestMatch.confidence
                tracked.lastSeenTime = currentTime
                tracked.consecutiveFrames++
                matchedObjects.add(tracked)
                currentDetections.remove(bestMatch) // Remove used detection
            } else {
                // Object lost temporarily
                if (currentTime - tracked.lastSeenTime > maxDisappearedTimeMs) {
                    it.remove() // Remove if lost for too long
                }
            }
        }

        // 2. Add new objects
        for (detection in currentDetections) {
            val newTrack = TrackedObject(
                id = nextId++,
                label = detection.label,
                boundingBox = detection.boundingBox,
                confidence = detection.confidence,
                lastSeenTime = currentTime,
                firstSeenTime = currentTime
            )
            trackedObjects.add(newTrack)
            matchedObjects.add(newTrack)
        }

        return matchedObjects
    }

    private fun calculateIoU(boxA: RectF, boxB: RectF): Float {
        val xA = max(boxA.left, boxB.left)
        val yA = max(boxA.top, boxB.top)
        val xB = min(boxA.right, boxB.right)
        val yB = min(boxA.bottom, boxB.bottom)

        val interArea = max(0f, xB - xA) * max(0f, yB - yA)
        val boxAArea = boxA.width() * boxA.height()
        val boxBArea = boxB.width() * boxB.height()

        return interArea / (boxAArea + boxBArea - interArea)
    }
}
