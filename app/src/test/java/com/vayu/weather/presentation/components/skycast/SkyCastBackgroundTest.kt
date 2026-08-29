package com.vayu.weather.presentation.components.skycast

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the [SkyCastBackground] gradient output. The background must
 *   - be deterministic (same input → same output)
 *   - be quiet (not too bright — text on top of the gradient must remain
 *     legible; we use a luminance-based "is dark enough?" check)
 *   - differ across the major weather buckets so the user can sense the
 *     condition from the background alone
 */
class SkyCastBackgroundTest {

    @Test
    fun same_input_produces_same_gradient() {
        val a = SkyCastBackground.gradientFor(0, isDay = true)
        val b = SkyCastBackground.gradientFor(0, isDay = true)
        assertEquals(a, b)
    }

    @Test
    fun all_gradients_are_two_color_vertical() {
        // The background is rendered as a vertical gradient with two stops.
        for (code in listOf(0, 1, 2, 3, 45, 51, 61, 71, 80, 95)) {
            for (isDay in listOf(true, false)) {
                val g = SkyCastBackground.gradientFor(code, isDay)
                assertEquals("code=$code isDay=$isDay should be 2 stops", 2, g.size)
            }
        }
    }

    @Test
    fun all_gradients_are_dark_for_white_text() {
        // The dashboard draws white text on the background. Every gradient
        // stop must therefore have low luminance (< 0.5) so the text
        // remains readable. Luminance is the WCAG relative luminance
        // (0..1).
        for (code in listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 61, 63, 65, 71, 73, 75, 80, 81, 82, 95, 96, 99)) {
            for (isDay in listOf(true, false)) {
                val g = SkyCastBackground.gradientFor(code, isDay)
                for (c in g) {
                    assertTrue(
                        "code=$code isDay=$isDay stop=$c should be dark enough for white text (luminance < 0.5, got ${c.luminance()})",
                        c.luminance() < 0.5f
                    )
                }
            }
        }
    }

    @Test
    fun different_codes_can_produce_different_gradients() {
        // At least *some* conditions must be distinguishable. If every
        // gradient collapsed to the same color the dynamic background
        // would be useless.
        val g0 = SkyCastBackground.gradientFor(0, isDay = true)
        val g95 = SkyCastBackground.gradientFor(95, isDay = true)
        val g71 = SkyCastBackground.gradientFor(71, isDay = true)
        assertNotEquals(g0, g95)
        assertNotEquals(g0, g71)
        assertNotEquals(g95, g71)
    }

    @Test
    fun clear_night_is_darker_than_clear_day() {
        val day = SkyCastBackground.gradientFor(0, isDay = true)
        val night = SkyCastBackground.gradientFor(0, isDay = false)
        assertTrue(
            "Clear night should be darker than clear day (day=${day[0].luminance()}, night=${night[0].luminance()})",
            night[0].luminance() < day[0].luminance()
        )
    }
}
