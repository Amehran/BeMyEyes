package com.amehran.bemyeyes.domain.describer

import com.amehran.bemyeyes.domain.model.Detection
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class SceneDescriberTest {

    private val describer = SceneDescriber()

    @Test
    fun `should group items effectively`() {
        val rect = mockk<android.graphics.RectF>(relaxed = true)
        val detections = listOf(
            Detection("chair", 0.9f, rect),
            Detection("table", 0.9f, rect)
        )

        val description = describer.describe(detections)

        assertEquals("chair and table ahead", description)
    }

    @Test
    fun `should handle single item`() {
        val rect = mockk<android.graphics.RectF>(relaxed = true)
        val detections = listOf(
            Detection("chair", 0.9f, rect)
        )

        val description = describer.describe(detections)

        assertEquals("chair ahead", description)
    }

    @Test
    fun `should prioritize urgent items`() {
        val rect = mockk<android.graphics.RectF>(relaxed = true)
        val detections = listOf(
            Detection("person", 0.9f, rect),
            Detection("car", 0.9f, rect) // Urgent
        )

        val description = describer.describe(detections)

        // "Car" should come first because it's urgent
        assertEquals("car and person ahead", description)
    }

    @Test
    fun `should count multiple items`() {
        val rect = mockk<android.graphics.RectF>(relaxed = true)
        val detections = listOf(
            Detection("person", 0.9f, rect),
            Detection("person", 0.9f, rect),
            Detection("person", 0.9f, rect),
            Detection("chair", 0.9f, rect)
        )

        val description = describer.describe(detections)

        // Should group "person" -> "3 persons"
        assertEquals("3 persons and chair ahead", description)
    }

    @Test
    fun `should include distance in description`() {
        val rect = mockk<android.graphics.RectF>(relaxed = true)
        val detections = listOf(
            Detection("car", 0.9f, rect, distanceMeters = 5.2f)
        )

        val description = describer.describe(detections)

        assertEquals("car at 5.2 meters ahead", description)
    }

    @Test
    fun `should group with min distance`() {
        val rect = mockk<android.graphics.RectF>(relaxed = true)
        val detections = listOf(
            Detection("car", 0.9f, rect, distanceMeters = 10f),
            Detection("car", 0.9f, rect, distanceMeters = 3.5f)
        )

        val description = describer.describe(detections)

        assertEquals("2 cars, closest at 3.5 meters ahead", description)
    }
}
