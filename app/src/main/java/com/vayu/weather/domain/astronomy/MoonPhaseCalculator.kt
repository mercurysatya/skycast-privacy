package com.vayu.weather.domain.astronomy

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Deterministic astronomical moon-phase calculator.
 *
 * Uses a Conway/Schaefer-style approximation: lunar age = days since a known
 * reference new moon, modulo the mean synodic month (29.5305882 days). This
 * is accurate to within roughly ±1 day, which is sufficient for a daily
 * "what phase is the moon in today" display.
 *
 * The phase is a continuous fraction in [0.0, 1.0):
 *   0.0  = new moon
 *   0.25 = first quarter (50% illuminated, waxing)
 *   0.5  = full moon (100% illuminated)
 *   0.75 = last/third quarter (50% illuminated, waning)
 *   1.0  = new moon again
 *
 * Illumination follows the standard cosine model:
 *   illum = (1 - cos(2πφ)) / 2
 *
 * Phase-name classification is based on the *illumination percentage* and
 * the *waxing/waning direction* (not on arbitrary φ boundaries), so the
 * named phase always matches the displayed icon and illumination.
 *
 * This calculator is **hemisphere-agnostic**: the phase name and illumination
 * are the same for every observer on Earth. Only the apparent orientation
 * of the illuminated limb depends on the observer's latitude.
 */
object MoonPhaseCalculator {

    /** Mean synodic month in days (new moon to new moon). */
    const val SYNODIC_MONTH_DAYS: Double = 29.5305882

    /**
     * Reference new moon: 11 January 2024 at 11:57 UTC. This is a recent,
     * well-documented new moon from the US Naval Observatory. Using a
     * recent reference minimises the cumulative drift of the Conway/
     * Schaefer mean-synodic-month approximation (which can accumulate
     * several days of error over decades).
     */
    private val REFERENCE_NEW_MOON_EPOCH_SECONDS: Long =
        LocalDateTime.of(2024, 1, 11, 11, 57).toEpochSecond(ZoneOffset.UTC)

    /**
     * Direction the moon is moving through its phase cycle. New moon and
     * full moon are [TRANSITION] points (not strictly waxing or waning).
     */
    enum class Direction { WAXING, WANING, TRANSITION }

    /** The 8 named moon phases used by the UI. */
    enum class PhaseName(
        val displayName: String,
        val direction: Direction
    ) {
        NEW_MOON("New moon", Direction.TRANSITION),
        WAXING_CRESCENT("Waxing crescent", Direction.WAXING),
        FIRST_QUARTER("First quarter", Direction.WAXING),
        WAXING_GIBBOUS("Waxing gibbous", Direction.WAXING),
        FULL_MOON("Full moon", Direction.TRANSITION),
        WANING_GIBBOUS("Waning gibbous", Direction.WANING),
        LAST_QUARTER("Last quarter", Direction.WANING),
        WANING_CRESCENT("Waning crescent", Direction.WANING);

        val isWaxing: Boolean get() = direction == Direction.WAXING
        val isWaning: Boolean get() = direction == Direction.WANING
    }

    /**
     * Full moon-phase result for a given instant.
     *
     * @param phaseFraction continuous phase in [0.0, 1.0)
     * @param illuminationFraction illuminated fraction of the visible disc, in [0.0, 1.0]
     * @param phaseAngle phase angle in radians, 0 = new moon, π = full moon
     * @param phaseName one of the 8 named phases
     * @param ageDays lunar age in days since the previous new moon, in [0.0, ~29.53)
     * @param date the date this result applies to
     */
    data class MoonPhase(
        val phaseFraction: Double,
        val illuminationFraction: Double,
        val phaseAngle: Double,
        val phaseName: PhaseName,
        val ageDays: Double,
        val date: LocalDate
    )

    /**
     * Compute the moon phase for a given [LocalDate]. The date is anchored
     * to noon UTC so the result is stable across the entire calendar day
     * regardless of the observer's timezone. Noon UTC is far from any
     * likely reference new moon time, giving the most representative phase
     * for the "middle" of the date.
     */
    fun computeForDate(date: LocalDate, zoneOffset: ZoneOffset = ZoneOffset.UTC): MoonPhase {
        val instant = date.atTime(12, 0).atOffset(zoneOffset).toInstant()
        return computeForInstant(instant, date)
    }

    /**
     * Compute the moon phase for a given [Instant] in UTC. The associated
     * [LocalDate] is derived from the instant's UTC date so the result is
     * always anchored to the astronomical day, not the local day.
     */
    fun computeForInstant(
        instant: Instant,
        referenceDate: LocalDate? = null
    ): MoonPhase {
        val epochSeconds = instant.epochSecond
        val daysSinceReference = (epochSeconds - REFERENCE_NEW_MOON_EPOCH_SECONDS) / 86_400.0
        val ageDays = ((daysSinceReference % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS
        val phaseFraction = ageDays / SYNODIC_MONTH_DAYS

        val phaseAngle = phaseFraction * 2.0 * PI
        val illuminationFraction = (1.0 - cos(phaseAngle)) / 2.0

        val phaseName = classifyPhase(phaseFraction, illuminationFraction)
        val date = referenceDate ?: instant.atOffset(ZoneOffset.UTC).toLocalDate()

        return MoonPhase(
            phaseFraction = phaseFraction,
            illuminationFraction = illuminationFraction,
            phaseAngle = phaseAngle,
            phaseName = phaseName,
            ageDays = ageDays,
            date = date
        )
    }

    /**
     * Classify a continuous phase into one of the 8 named phases.
     *
     * Classification uses the *illumination percentage* and the *waxing/
     * waning direction* (not arbitrary φ boundaries). The Conway/Schaefer
     * approximation is accurate to roughly ±1 day, so we use a tolerance
     * band around 50% illumination to catch quarter phases correctly.
     *
     * Boundaries (illumination percentage):
     *   - 0%   <  1%  : New moon
     *   - 1%   ≤  40% : Crescent (waxing or waning)
     *   - 40%  ≤  60% : Quarter (first or last)
     *   - 60%  <  99% : Gibbous (waxing or waning)
     *   - 99%  ≤ 100% : Full moon
     *
     * Waxing vs waning is decided by whether the phase fraction is in the
     * first or second half of the synodic month. This is the same direction
     * for every observer on Earth.
     */
    private fun classifyPhase(phaseFraction: Double, illuminationFraction: Double): PhaseName {
        val isWaxing = phaseFraction < 0.5
        val illumPct = illuminationFraction * 100.0

        return when {
            illumPct < 1.0 -> PhaseName.NEW_MOON
            illumPct > 99.0 -> PhaseName.FULL_MOON
            illumPct in 1.0..40.0 && isWaxing -> PhaseName.WAXING_CRESCENT
            illumPct in 1.0..40.0 && !isWaxing -> PhaseName.WANING_CRESCENT
            illumPct in 40.0..60.0 && isWaxing -> PhaseName.FIRST_QUARTER
            illumPct in 40.0..60.0 && !isWaxing -> PhaseName.LAST_QUARTER
            illumPct in 60.0..99.0 && isWaxing -> PhaseName.WAXING_GIBBOUS
            illumPct in 60.0..99.0 && !isWaxing -> PhaseName.WANING_GIBBOUS
            // Fallback: use phase fraction for edge cases at illumination
            // boundaries where the tolerance bands overlap.
            phaseFraction < 0.125 || phaseFraction > 0.875 -> PhaseName.NEW_MOON
            phaseFraction < 0.375 -> if (isWaxing) PhaseName.WAXING_CRESCENT else PhaseName.FIRST_QUARTER
            phaseFraction < 0.625 -> if (isWaxing) PhaseName.WAXING_GIBBOUS else PhaseName.WANING_GIBBOUS
            phaseFraction < 0.875 -> if (isWaxing) PhaseName.FIRST_QUARTER else PhaseName.LAST_QUARTER
            else -> PhaseName.FULL_MOON
        }
    }

    /**
     * For UI: returns the illumination as an integer percentage in [0, 100].
     */
    fun illuminationPercent(phase: MoonPhase): Int =
        (phase.illuminationFraction * 100.0).toInt().coerceIn(0, 100)

    /**
     * For UI: the normalized terminator x-offset used by the moon-renderer
     * to draw the elliptical terminator. Returns a value in [-1, +1]:
     *   -1.0 = terminator at the leftmost edge (dark on right, waxing)
     *   +1.0 = terminator at the rightmost edge (dark on left, waning)
     *    0.0 = terminator through the center (quarter phase)
     *
     * The formula uses the actual phase angle (not the derived phaseFraction)
     * so the visual is symmetric around full/new moon.
     */
    fun terminatorOffset(phase: MoonPhase): Double {
        // The terminator is a half-ellipse whose width follows cos(phaseAngle).
        // At new moon (angle=0): cos=1, terminator is at the right edge.
        // At first quarter (angle=π/2): cos=0, terminator through center.
        // At full moon (angle=π): cos=-1, terminator at the left edge.
        // At last quarter (angle=3π/2): cos=0, terminator through center.
        return cos(phase.phaseAngle)
    }
}
