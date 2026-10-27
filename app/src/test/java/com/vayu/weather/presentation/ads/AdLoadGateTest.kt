package com.vayu.weather.presentation.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for [AdLoadGate]: exactly one in-flight load may be claimed
 * at a time, and release() frees the slot for the next load.
 */
class AdLoadGateTest {

    @Test
    fun `first acquire claims the slot`() {
        val gate = AdLoadGate()
        assertTrue(gate.acquire())
    }

    @Test
    fun `concurrent acquires are collapsed into one`() {
        val gate = AdLoadGate()
        assertTrue(gate.acquire())
        // Both preload triggers (ON_RESUME + UMP consent callback)
        assertFalse(gate.acquire())
        assertFalse(gate.acquire())
    }

    @Test
    fun `release frees the slot for the next load`() {
        val gate = AdLoadGate()
        assertTrue(gate.acquire())
        gate.release()
        assertTrue(gate.acquire())
    }

    @Test
    fun `failed then retried load works after double release`() {
        val gate = AdLoadGate()
        assertTrue(gate.acquire())
        gate.release() // onAdFailedToLoad
        gate.release() // idempotent defensive release
        assertTrue(gate.acquire())
    }

    @Test
    fun `release before acquire does not break the next cycle`() {
        val gate = AdLoadGate()
        gate.release()
        assertTrue(gate.acquire())
    }
}
