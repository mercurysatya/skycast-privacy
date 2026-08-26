package com.vayu.weather.presentation.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseMapStyleTest {

    @Test
    fun `STREET has correct display name`() {
        assertEquals("Street", BaseMapStyle.STREET.displayName)
    }

    @Test
    fun `SATELLITE has correct display name`() {
        assertEquals("Satellite", BaseMapStyle.SATELLITE.displayName)
    }

    @Test
    fun `VOYAGER has correct display name`() {
        assertEquals("Voyager", BaseMapStyle.VOYAGER.displayName)
    }

    @Test
    fun `DARK has correct display name`() {
        assertEquals("Dark", BaseMapStyle.DARK.displayName)
    }

    @Test
    fun `STREET style URL contains positron`() {
        assertTrue(BaseMapStyle.STREET.styleUrl.contains("positron"))
    }

    @Test
    fun `SATELLITE style URL is a valid URL`() {
        assertTrue(BaseMapStyle.SATELLITE.styleUrl.startsWith("https://"))
    }

    @Test
    fun `VOYAGER style URL contains voyager`() {
        assertTrue(BaseMapStyle.VOYAGER.styleUrl.contains("voyager"))
    }

    @Test
    fun `DARK style URL contains dark-matter`() {
        assertTrue(BaseMapStyle.DARK.styleUrl.contains("dark-matter"))
    }

    @Test
    fun `all styles have 4 entries`() {
        assertEquals(4, BaseMapStyle.entries.size)
    }

    @Test
    fun `all style URLs start with https`() {
        BaseMapStyle.entries.forEach { style ->
            assertTrue("${style.name} URL should start with https://", style.styleUrl.startsWith("https://"))
        }
    }
}
