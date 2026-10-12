package com.vayu.weather

import com.vayu.weather.data.repository.WeatherRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Regression tests for the weather-cache location key.
 *
 * The old key used raw Double coordinates ("48.8566,2.3522"), so every GPS
 * fix (which varies slightly) produced a new cache row, the 30-minute TTL
 * never matched, and the table grew without bound.
 */
class WeatherCacheKeyTest {

    @Test
    fun `nearby GPS fixes share the same cache key`() {
        val a = WeatherRepositoryImpl.cacheKeyFor(12.971598, 77.594566)
        val b = WeatherRepositoryImpl.cacheKeyFor(12.971634, 77.594501)
        assertEquals("Sub-100m drift must hit the same cache row", a, b)
    }

    @Test
    fun `distinct cities get distinct cache keys`() {
        val bengaluru = WeatherRepositoryImpl.cacheKeyFor(12.9716, 77.5946)
        val delhi = WeatherRepositoryImpl.cacheKeyFor(28.6139, 77.2090)
        assertNotEquals(bengaluru, delhi)
    }

    @Test
    fun `cache key is locale independent`() {
        // German/Turkish locales format decimals with commas; a key like
        // "12,97,77,59" would collide across coordinates.
        val key = WeatherRepositoryImpl.cacheKeyFor(12.9716, 77.5946)
        val parts = key.split(",")
        assertEquals(2, parts.size)
        parts.forEach { part ->
            val number = part.toDoubleOrNull()
            assertNotNull("Each key part must parse as a plain number: $part", number)
        }
    }

    @Test
    fun `negative coordinates keep their sign`() {
        val key = WeatherRepositoryImpl.cacheKeyFor(-33.8688, -151.2093)
        assertEquals("-33.869,-151.209", key)
    }
}
