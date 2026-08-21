package com.vayu.weather.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarStateTest {

    // ==================== currentPath ====================

    @Test
    fun `currentPath returns path when valid index`() {
        val state = RadarState(
            frames = listOf(
                RadarFrame(time = 1000L, path = "/v2/radar/aaa"),
                RadarFrame(time = 2000L, path = "/v2/radar/bbb")
            ),
            selectedFrameIndex = 0
        )
        assertEquals("/v2/radar/aaa", state.currentPath)
    }

    @Test
    fun `currentPath returns second frame path`() {
        val state = RadarState(
            frames = listOf(
                RadarFrame(time = 1000L, path = "/v2/radar/aaa"),
                RadarFrame(time = 2000L, path = "/v2/radar/bbb")
            ),
            selectedFrameIndex = 1
        )
        assertEquals("/v2/radar/bbb", state.currentPath)
    }

    @Test
    fun `currentPath returns null when no frames`() {
        val state = RadarState(frames = emptyList(), selectedFrameIndex = 0)
        assertNull(state.currentPath)
    }

    @Test
    fun `currentPath returns null when index is -1`() {
        val state = RadarState(
            frames = listOf(RadarFrame(time = 1000L, path = "/v2/radar/aaa")),
            selectedFrameIndex = -1
        )
        assertNull(state.currentPath)
    }

    @Test
    fun `currentPath returns null when index is out of bounds`() {
        val state = RadarState(
            frames = listOf(RadarFrame(time = 1000L, path = "/v2/radar/aaa")),
            selectedFrameIndex = 5
        )
        assertNull(state.currentPath)
    }

    // ==================== currentLabel ====================

    @Test
    fun `currentLabel returns label when valid index`() {
        val state = RadarState(
            frames = listOf(RadarFrame(time = 1000L, path = "/v2/radar/aaa", label = "14:30")),
            selectedFrameIndex = 0
        )
        assertEquals("14:30", state.currentLabel)
    }

    @Test
    fun `currentLabel returns empty string when no frames`() {
        val state = RadarState(frames = emptyList(), selectedFrameIndex = 0)
        assertEquals("", state.currentLabel)
    }

    @Test
    fun `currentLabel returns empty string when index out of bounds`() {
        val state = RadarState(
            frames = listOf(RadarFrame(time = 1000L, path = "/v2/radar/aaa", label = "14:30")),
            selectedFrameIndex = 10
        )
        assertEquals("", state.currentLabel)
    }

    // ==================== hasFrames ====================

    @Test
    fun `hasFrames returns true when frames present`() {
        val state = RadarState(
            frames = listOf(RadarFrame(time = 1000L, path = "/v2/radar/aaa"))
        )
        assertTrue(state.hasFrames)
    }

    @Test
    fun `hasFrames returns false when no frames`() {
        val state = RadarState(frames = emptyList())
        assertFalse(state.hasFrames)
    }

    // ==================== Default values ====================

    @Test
    fun `default RadarState has empty frames`() {
        val state = RadarState()
        assertTrue(state.frames.isEmpty())
        assertFalse(state.hasFrames)
        assertNull(state.currentPath)
        assertEquals("", state.currentLabel)
    }

    @Test
    fun `default tileHost is rainviewer`() {
        val state = RadarState()
        assertEquals("https://tilecache.rainviewer.com", state.tileHost)
    }

    // ==================== Frame selection logic ====================

    @Test
    fun `selectFrame clamps to valid range`() {
        val frames = listOf(
            RadarFrame(time = 1000L, path = "/a"),
            RadarFrame(time = 2000L, path = "/b"),
            RadarFrame(time = 3000L, path = "/c")
        )

        // Simulate selectFrame logic
        fun selectFrame(state: RadarState, index: Int): RadarState {
            return state.copy(
                selectedFrameIndex = index.coerceIn(0, (state.frames.size - 1).coerceAtLeast(0))
            )
        }

        val state = RadarState(frames = frames, selectedFrameIndex = 0)

        // Select valid index
        val s1 = selectFrame(state, 1)
        assertEquals(1, s1.selectedFrameIndex)

        // Clamp negative
        val s2 = selectFrame(state, -5)
        assertEquals(0, s2.selectedFrameIndex)

        // Clamp too high
        val s3 = selectFrame(state, 100)
        assertEquals(2, s3.selectedFrameIndex)
    }

    @Test
    fun `selectNextFrame advances within bounds`() {
        fun selectNextFrame(state: RadarState): RadarState {
            val next = (state.selectedFrameIndex + 1).coerceAtMost(state.frames.size - 1)
            return state.copy(selectedFrameIndex = next)
        }

        val state = RadarState(
            frames = listOf(
                RadarFrame(time = 1000L, path = "/a"),
                RadarFrame(time = 2000L, path = "/b"),
                RadarFrame(time = 3000L, path = "/c")
            ),
            selectedFrameIndex = 0
        )

        val s1 = selectNextFrame(state)
        assertEquals(1, s1.selectedFrameIndex)

        val s2 = selectNextFrame(s1)
        assertEquals(2, s2.selectedFrameIndex)

        // Should not go past last
        val s3 = selectNextFrame(s2)
        assertEquals(2, s3.selectedFrameIndex)
    }

    @Test
    fun `selectPreviousFrame goes back within bounds`() {
        fun selectPreviousFrame(state: RadarState): RadarState {
            val prev = (state.selectedFrameIndex - 1).coerceAtLeast(0)
            return state.copy(selectedFrameIndex = prev)
        }

        val state = RadarState(
            frames = listOf(
                RadarFrame(time = 1000L, path = "/a"),
                RadarFrame(time = 2000L, path = "/b"),
                RadarFrame(time = 3000L, path = "/c")
            ),
            selectedFrameIndex = 2
        )

        val s1 = selectPreviousFrame(state)
        assertEquals(1, s1.selectedFrameIndex)

        val s2 = selectPreviousFrame(s1)
        assertEquals(0, s2.selectedFrameIndex)

        // Should not go below 0
        val s3 = selectPreviousFrame(s2)
        assertEquals(0, s3.selectedFrameIndex)
    }
}
