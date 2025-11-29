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
        every { rect.centerX() } returns 320f // Center of 640 width? No, we changed to 320 width.
        // Wait, I changed the code to use 320f as image width in Detection.kt.
        // So center is 160f.
        every { rect.centerX() } returns 160f
        every { rect.height() } returns 200f // 200/320 = 0.625 > 0.6 -> very close
        
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
        every { rect.centerX() } returns 50f // < 320 * 0.35 = 112
        every { rect.height() } returns 50f // 50/320 = 0.15 < 0.3 -> far (empty string)

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
        every { rect.centerX() } returns 300f // > 320 * 0.65 = 208
        every { rect.height() } returns 120f // 120/320 = 0.375 -> close (> 0.3 but < 0.6)

        val detection = Detection("Table", 0.7f, rect)

        // When
        val description = detection.getDescription()

        // Then
        assertEquals("Table, to the right, close", description)
    }
}
