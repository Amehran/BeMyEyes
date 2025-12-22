package com.amehran.bemyeyes.domain.tracker

import com.amehran.bemyeyes.domain.model.Detection
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionTrackerTest {

    // I'll assume the class will be created in this package
    private val tracker = DetectionTracker()

    @Test
    fun `should wait 3 frames before reporting new object`() {
        val rect = mockk<android.graphics.RectF>(relaxed = true)
        val detection = Detection("chair", 0.9f, rect)

        // Frame 1
        val result1 = tracker.process(listOf(detection))
        assertTrue("Frame 1 should be empty", result1.isEmpty())

        // Frame 2
        val result2 = tracker.process(listOf(detection))
        assertTrue("Frame 2 should be empty", result2.isEmpty())

        // Frame 3 (Stability Threshold Reached)
        val result3 = tracker.process(listOf(detection))
        assertEquals("Frame 3 should have object", 1, result3.size)
        assertEquals("chair", result3[0].label)
    }

    @Test
    fun `should keep object alive if missing for 1 frame`() {
        val rect = mockk<android.graphics.RectF>(relaxed = true)
        val detection = Detection("chair", 0.9f, rect)

        // Establish the object (3 frames)
        tracker.process(listOf(detection))
        tracker.process(listOf(detection))
        tracker.process(listOf(detection))

        // Missing Frame (Object drops out)
        val resultMissing = tracker.process(emptyList())
        // Should still report it because it's "buffered"
        assertEquals("Should stay alive for 1 missing frame", 1, resultMissing.size)
        assertEquals("chair", resultMissing[0].label)

        // Reappears next frame
        val resultReappear = tracker.process(listOf(detection))
        assertEquals("Should continue tracking", 1, resultReappear.size)
    }

    @Test
    fun `should drop object if missing for 5 frames`() {
        val rect = mockk<android.graphics.RectF>(relaxed = true)
        val detection = Detection("chair", 0.9f, rect)

        // Establish object
        tracker.process(listOf(detection))
        tracker.process(listOf(detection))
        tracker.process(listOf(detection))

        // Miss for 4 frames (Still alive)
        repeat(4) {
            val res = tracker.process(emptyList())
            assertEquals("Should survive frame $it", 1, res.size)
        }

        // Miss for 5th frame (Drop)
        val resultDropped = tracker.process(emptyList())
        assertTrue("Should drop after 5 frames", resultDropped.isEmpty())
    }
}
