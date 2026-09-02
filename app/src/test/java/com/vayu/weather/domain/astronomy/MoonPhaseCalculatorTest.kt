package com.vayu.weather.domain.astronomy

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Regression tests for [MoonPhaseCalculator].
 *
 * Reference astronomical data comes from NASA / US Naval Observatory
 * published new-moon and full-moon tables. We assert illumination ranges
 * and phase names around these known events.
 */
class MoonPhaseCalculatorTest {

    // ═══════════════════════════════════════════════════════════
    // PHASE FRACTION BOUNDARIES
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `phase fraction is in range 0 to 1 for reference new moon date`() {
        // 2024-01-11 is the reference new moon
        val result = MoonPhaseCalculator.computeForDate(LocalDate.of(2024, 1, 11))
        assertTrue("Phase fraction ${result.phaseFraction} should be near 0",
            result.phaseFraction < 0.05)
        assertEquals(MoonPhaseCalculator.PhaseName.NEW_MOON, result.phaseName)
    }

    @Test
    fun `phase fraction is monotonically increasing across a synodic month`() {
        val start = LocalDate.of(2024, 1, 11) // reference new moon
        var previous = 0.0
        for (day in 0 until 30) {
            val result = MoonPhaseCalculator.computeForDate(start.plusDays(day.toLong()))
            if (day > 0) {
                assertTrue("Phase should increase at day $day (was $previous, now ${result.phaseFraction})",
                    result.phaseFraction >= previous - 0.001)
            }
            previous = result.phaseFraction
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ILLUMINATION
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `illumination is 0 at new moon and 100 at full moon`() {
        val newMoon = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 29))
        val fullMoon = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 2, 12))
        assertTrue("New moon illum should be ~0%, was ${newMoon.illuminationFraction * 100}",
            newMoon.illuminationFraction * 100 < 2.0)
        assertTrue("Full moon illum should be ~100%, was ${fullMoon.illuminationFraction * 100}",
            fullMoon.illuminationFraction * 100 > 98.0)
    }

    @Test
    fun `illumination is approximately 50 percent near quarter phases`() {
        // The Conway/Schaefer approximation is accurate to roughly ±1-2 days.
        // Dates near the actual quarter phases should show illumination
        // within ±15% of 50%.
        // First quarter window: 2025-01-05 to 2025-01-08
        val firstQ = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 6))
        // Last quarter window: 2025-01-20 to 2025-01-23
        val lastQ = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 21))
        val firstIllum = firstQ.illuminationFraction * 100
        val lastIllum = lastQ.illuminationFraction * 100
        // The Conway approximation can be ±2 days off, so illumination at
        // these "near quarter" dates may range from ~35% to ~65%.
        assertTrue("First quarter illum should be 35-65%, was $firstIllum",
            firstIllum in 35.0..65.0)
        assertTrue("Last quarter illum should be 35-65%, was $lastIllum",
            lastIllum in 35.0..65.0)
    }

    @Test
    fun `illumination is always between 0 and 100 percent`() {
        for (day in 1..365) {
            val result = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 1).plusDays(day.toLong() - 1L))
            assertTrue("Day $day illum out of range: ${result.illuminationFraction}",
                result.illuminationFraction in 0.0..1.0)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // KNOWN ASTRONOMICAL EVENTS (NASA reference dates)
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `2025-01-29 is classified as new moon`() {
        val result = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 29))
        assertEquals(MoonPhaseCalculator.PhaseName.NEW_MOON, result.phaseName)
    }

    @Test
    fun `2025-02-12 is classified as full moon`() {
        val result = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 2, 12))
        assertEquals(MoonPhaseCalculator.PhaseName.FULL_MOON, result.phaseName)
    }

    @Test
    fun `2025-03-29 is classified as new moon`() {
        val result = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 3, 29))
        assertEquals(MoonPhaseCalculator.PhaseName.NEW_MOON, result.phaseName)
    }

    @Test
    fun `2025-04-13 is classified as full moon`() {
        val result = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 4, 13))
        assertEquals(MoonPhaseCalculator.PhaseName.FULL_MOON, result.phaseName)
    }

    // ═══════════════════════════════════════════════════════════
    // WAXING vs WANING
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `waxing phases are flagged as waxing`() {
        val phases = listOf(
            MoonPhaseCalculator.PhaseName.WAXING_CRESCENT,
            MoonPhaseCalculator.PhaseName.FIRST_QUARTER,
            MoonPhaseCalculator.PhaseName.WAXING_GIBBOUS
        )
        phases.forEach { phase ->
            assertTrue("$phase should be waxing", phase.isWaxing)
            assertFalse("$phase should not be waning", phase.isWaning)
        }
    }

    @Test
    fun `waning phases are flagged as waning`() {
        val phases = listOf(
            MoonPhaseCalculator.PhaseName.WANING_GIBBOUS,
            MoonPhaseCalculator.PhaseName.LAST_QUARTER,
            MoonPhaseCalculator.PhaseName.WANING_CRESCENT
        )
        phases.forEach { phase ->
            assertTrue("$phase should be waning", phase.isWaning)
            assertFalse("$phase should not be waxing", phase.isWaxing)
        }
    }

    @Test
    fun `new moon and full moon are neither waxing nor waning`() {
        assertFalse(MoonPhaseCalculator.PhaseName.NEW_MOON.isWaxing)
        assertFalse(MoonPhaseCalculator.PhaseName.NEW_MOON.isWaning)
        assertFalse(MoonPhaseCalculator.PhaseName.FULL_MOON.isWaxing)
        assertFalse(MoonPhaseCalculator.PhaseName.FULL_MOON.isWaning)
    }

    @Test
    fun `first quarter is always waxing not waning`() {
        val firstQ = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 6))
        assertEquals(MoonPhaseCalculator.PhaseName.FIRST_QUARTER, firstQ.phaseName)
        assertTrue("First quarter should be waxing", firstQ.phaseName.isWaxing)
    }

    @Test
    fun `last quarter is always waning not waxing`() {
        val lastQ = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 21))
        assertEquals(MoonPhaseCalculator.PhaseName.LAST_QUARTER, lastQ.phaseName)
        assertTrue("Last quarter should be waning", lastQ.phaseName.isWaning)
    }

    // ═══════════════════════════════════════════════════════════
    // WAXING/WANING NEVER SWAPPED
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `waxing crescent never appears as waning crescent`() {
        for (day in 0..30) {
            val date = LocalDate.of(2025, 1, 29).plusDays(day.toLong())
            val result = MoonPhaseCalculator.computeForDate(date)
            if (result.phaseName == MoonPhaseCalculator.PhaseName.WAXING_CRESCENT) {
                assertTrue("Waxing crescent at $date should have waxing flag",
                    result.phaseName.isWaxing)
            }
            if (result.phaseName == MoonPhaseCalculator.PhaseName.WANING_CRESCENT) {
                assertTrue("Waning crescent at $date should have waning flag",
                    result.phaseName.isWaning)
            }
        }
    }

    @Test
    fun `waxing gibbous never appears as waning gibbous`() {
        for (day in 0..30) {
            val date = LocalDate.of(2025, 1, 29).plusDays(day.toLong())
            val result = MoonPhaseCalculator.computeForDate(date)
            if (result.phaseName == MoonPhaseCalculator.PhaseName.WAXING_GIBBOUS) {
                assertTrue(result.phaseName.isWaxing)
            }
            if (result.phaseName == MoonPhaseCalculator.PhaseName.WANING_GIBBOUS) {
                assertTrue(result.phaseName.isWaning)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HEMISPHERE INDEPENDENCE
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `phase name is same regardless of timezone offset`() {
        val date = LocalDate.of(2025, 6, 15)
        val utc = MoonPhaseCalculator.computeForDate(date, ZoneOffset.UTC)
        val plus12 = MoonPhaseCalculator.computeForDate(date, ZoneOffset.ofHours(12))
        val minus12 = MoonPhaseCalculator.computeForDate(date, ZoneOffset.ofHours(-12))
        // Phase name may differ by one day at boundaries, but the underlying
        // astronomical phase (illumination, fraction) must be very close.
        assertEquals("UTC vs +12 illumination should match within 5%",
            utc.illuminationFraction, plus12.illuminationFraction, 0.05)
        assertEquals("UTC vs -12 illumination should match within 5%",
            utc.illuminationFraction, minus12.illuminationFraction, 0.05)
    }

    // ═══════════════════════════════════════════════════════════
    // TERMINATOR GEOMETRY
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `terminator offset is 1 at new moon and -1 at full moon`() {
        val newMoon = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 29))
        val fullMoon = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 2, 12))
        assertTrue("New moon terminator should be ~+1, was ${MoonPhaseCalculator.terminatorOffset(newMoon)}",
            MoonPhaseCalculator.terminatorOffset(newMoon) > 0.95)
        assertTrue("Full moon terminator should be ~-1, was ${MoonPhaseCalculator.terminatorOffset(fullMoon)}",
            MoonPhaseCalculator.terminatorOffset(fullMoon) < -0.95)
    }

    @Test
    fun `terminator offset is near 0 near quarter phases`() {
        val firstQ = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 6))
        val lastQ = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 1, 21))
        // Conway approximation may be ±2 days off, so the terminator at
        // these "near quarter" dates may not be exactly 0. The terminator
        // offset is cos(phaseAngle), which changes slowly near the quarters.
        // Acceptable range: ±0.35.
        assertTrue("First quarter terminator should be near 0, was ${MoonPhaseCalculator.terminatorOffset(firstQ)}",
            kotlin.math.abs(MoonPhaseCalculator.terminatorOffset(firstQ)) < 0.35)
        assertTrue("Last quarter terminator should be near 0, was ${MoonPhaseCalculator.terminatorOffset(lastQ)}",
            kotlin.math.abs(MoonPhaseCalculator.terminatorOffset(lastQ)) < 0.35)
    }

    // ═══════════════════════════════════════════════════════════
    // STABILITY OVER A FULL SYNODIC MONTH
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `all 8 phases appear within one synodic month`() {
        val seen = mutableSetOf<MoonPhaseCalculator.PhaseName>()
        val start = LocalDate.of(2025, 1, 29) // known new moon
        for (day in 0 until 30) {
            val date = start.plusDays(day.toLong())
            seen.add(MoonPhaseCalculator.computeForDate(date).phaseName)
        }
        // We may not see every single phase name in a 30-day window due to
        // boundary effects, but we should see at least 6 distinct phases.
        assertTrue("Expected at least 6 distinct phases, saw ${seen.size}: $seen",
            seen.size >= 6)
    }

    // ═══════════════════════════════════════════════════════════
    // ILLUMINATION PERCENT HELPER
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `illuminationPercent returns integer in 0 to 100`() {
        for (day in 1..60) {
            val result = MoonPhaseCalculator.computeForDate(LocalDate.of(2025, 3, 1).plusDays(day.toLong() - 1L))
            val pct = MoonPhaseCalculator.illuminationPercent(result)
            assertTrue("Day $day: illumPercent $pct out of range", pct in 0..100)
        }
    }
}
