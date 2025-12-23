package com.amehran.bemyeyes.domain.model

import android.graphics.RectF
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectionTest {

    @Test
    fun `getDescription returns correct description for center close object`() {
        // Given
        val rect = mockk<RectF>()
        every { rect.centerX() } returns 0.5f // Center (0.5 within 0.35-0.65)
        every { rect.height() } returns 0.7f // 0.7 > 0.6 -> very close
        
        val detection = Detection("Person", 0.9f, rect)

        // When
        val description = detection.getDescription()

        // Then
        assertEquals("Person, in front, very close", description)
    }

    @Test
    fun `getDescription returns correct description for left far object`() {
        // Given
        val rect = mockk<RectF>()
        every { rect.centerX() } returns 0.2f // < 0.35 -> left
        every { rect.height() } returns 0.15f // < 0.3 -> far (empty string)

        val detection = Detection("Chair", 0.8f, rect)

        // When
        val description = detection.getDescription()

        // Then
        assertEquals("Chair, to the left", description)
    }

    @Test
    fun `getDescription returns correct description for right close object`() {
        // Given
        val rect = mockk<RectF>()
        every { rect.centerX() } returns 0.8f // > 0.65 -> right
        every { rect.height() } returns 0.4f // > 0.3 and < 0.6 -> close

        val detection = Detection("Table", 0.7f, rect)

        // When
        val description = detection.getDescription()

        // Then
        assertEquals("Table, to the right, close", description)
    }

    @Test
    fun `getDescription uses distanceMeters when available`() {
        val rect = mockk<RectF>(relaxed = true)
        every { rect.centerX() } returns 0.5f
        
        // Case 1: < 2m (Very Close)
        val d1 = Detection("car", 0.9f, rect, distanceMeters = 1.5f)
        assertEquals("Caution: car, in front, very close", d1.getDescription())

        // Case 2: < 5m (Close)
        val d2 = Detection("person", 0.9f, rect, distanceMeters = 3.5f)
        assertEquals("person, in front, close", d2.getDescription())

        // Case 3: > 5m (Specific distance)
        val d3 = Detection("bus", 0.9f, rect, distanceMeters = 10.0f)
        assertEquals("Caution: bus, in front, at 10.0m", d3.getDescription())
    }
}
