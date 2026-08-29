package com.vayu.weather.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class SkyCastColorsTest {

    @Test
    fun usAqiCategories() {
        assertEquals(AqiGood, SkyCastColors.forUsAqi(20))
        assertEquals(AqiFair, SkyCastColors.forUsAqi(80))
        assertEquals(AqiModerate, SkyCastColors.forUsAqi(120))
        assertEquals(AqiPoor, SkyCastColors.forUsAqi(180))
        assertEquals(AqiVeryPoor, SkyCastColors.forUsAqi(250))
        assertEquals(AqiSevere, SkyCastColors.forUsAqi(400))
    }

    @Test
    fun uvCategories() {
        assertEquals(UvLow, SkyCastColors.forUvIndex(1.0))
        assertEquals(UvModerate, SkyCastColors.forUvIndex(4.0))
        assertEquals(UvHigh, SkyCastColors.forUvIndex(7.0))
        assertEquals(UvVeryHigh, SkyCastColors.forUvIndex(9.0))
        assertEquals(UvExtreme, SkyCastColors.forUvIndex(12.0))
    }

    @Test
    fun windColorScale() {
        assertEquals(FreshGreen, SkyCastColors.forWindKph(5.0))
        assertEquals(AqiFair, SkyCastColors.forWindKph(20.0))
        assertEquals(UvModerate, SkyCastColors.forWindKph(45.0))
        assertEquals(UvVeryHigh, SkyCastColors.forWindKph(80.0))
        assertEquals(UvExtreme, SkyCastColors.forWindKph(120.0))
    }
}
