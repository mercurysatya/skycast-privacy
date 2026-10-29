package com.vayu.weather.presentation.map

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import kotlin.math.min

class MapOverlayDataTest {

    private fun features(data: GeoJsonData): List<Feature<*, *>> {
        val collection = (data as GeoJsonData.Features).geoJson as FeatureCollection<*, *>
        return collection.features
    }

    // ==================== Temperature overlay ====================

    @Test
    fun `temperature overlay creates one point per grid sample`() {
        val points = listOf(
            TempGridPoint(lat = 12.9, lon = 77.6, temp = 24.5),
            TempGridPoint(lat = 13.0, lon = 77.7, temp = 31.0)
        )

        val features = features(temperatureOverlayData(points))

        assertEquals(2, features.size)
        val geometry = features[0].geometry as Point
        assertEquals(77.6, geometry.longitude, 1e-9)
        assertEquals(12.9, geometry.latitude, 1e-9)
        val properties = features[0].properties as JsonObject
        assertEquals(24.5, properties["temp"]!!.jsonPrimitive.double, 1e-9)
    }

    @Test
    fun `temperature overlay is empty when the grid has no samples`() {
        assertTrue(features(temperatureOverlayData(emptyList())).isEmpty())
    }

    @Test
    fun `temperature colour matches the legend bands`() {
        assertEquals(TemperatureColdestColor, temperatureColor(-11.0))
        assertEquals(TemperatureColorStops[0].second, temperatureColor(-10.0))
        assertEquals(TemperatureColorStops[0].second, temperatureColor(-0.1))
        assertEquals(TemperatureColorStops[1].second, temperatureColor(0.0))
        assertEquals(TemperatureColorStops.last().second, temperatureColor(40.0))
    }

    // ==================== Wind overlay ====================

    @Test
    fun `wind overlay creates one line feature per sample with speed`() {
        val points = listOf(WindPoint(lat = 12.9, lon = 77.6, speed = 18.0, direction = 90.0))

        val features = features(windOverlayData(points, arrowLengthDegrees = 0.05))

        assertEquals(1, features.size)
        assertTrue(features[0].geometry is LineString)
        val properties = features[0].properties as JsonObject
        assertEquals(18.0, properties["speed"]!!.jsonPrimitive.double, 1e-9)
    }

    @Test
    fun `wind arrow points where the wind is going`() {
        val length = 0.2

        // Wind from the north blows south.
        val fromNorth = windArrowPositions(
            WindPoint(lat = 20.0, lon = 0.0, speed = 10.0, direction = 0.0),
            arrowLengthDegrees = length
        )
        assertTrue(fromNorth[1].latitude < fromNorth[0].latitude)

        // Wind from the east blows west.
        val fromEast = windArrowPositions(
            WindPoint(lat = 20.0, lon = 0.0, speed = 10.0, direction = 90.0),
            arrowLengthDegrees = length
        )
        assertTrue(fromEast[1].longitude < fromEast[0].longitude)

        // Wind from the south blows north.
        val fromSouth = windArrowPositions(
            WindPoint(lat = 20.0, lon = 0.0, speed = 10.0, direction = 180.0),
            arrowLengthDegrees = length
        )
        assertTrue(fromSouth[1].latitude > fromSouth[0].latitude)
    }

    @Test
    fun `wind arrow has a shaft and two arrowhead arms`() {
        val positions = windArrowPositions(
            WindPoint(lat = 20.0, lon = 0.0, speed = 10.0, direction = 45.0),
            arrowLengthDegrees = 0.2
        )

        assertEquals(5, positions.size)
        // The line retraces the tip so the two head arms stay in one LineString.
        assertEquals(positions[1], positions[3])
        assertFalse(positions[0] == positions[1])
    }

    @Test
    fun `wind colour matches the legend bands`() {
        assertEquals(WindCalmColor, windSpeedColor(4.9))
        assertEquals(WindColorStops[0].second, windSpeedColor(5.0))
        assertEquals(WindColorStops[1].second, windSpeedColor(15.0))
        assertEquals(WindColorStops[2].second, windSpeedColor(25.0))
        assertEquals(WindColorStops[3].second, windSpeedColor(35.0))
    }

    // ==================== Grid sizing ====================

    @Test
    fun `grid bounds cover the visible viewport`() {
        val bounds = gridBounds(
            BoundingBox(west = -123.5, south = 38.9, east = -123.0, north = 39.1)
        )

        assertEquals(39.0, bounds.centerLat, 1e-9)
        assertEquals(-123.25, bounds.centerLon, 1e-9)
        assertEquals(0.2, bounds.spanLat, 1e-9)
        assertEquals(0.5, bounds.spanLon, 1e-9)
    }

    @Test
    fun `grid bounds handle a viewport crossing the antimeridian`() {
        val bounds = gridBounds(
            BoundingBox(west = 170.0, south = -10.0, east = -170.0, north = 10.0)
        )

        assertEquals(20.0, bounds.spanLon, 1e-9)
        assertEquals(180.0, kotlin.math.abs(bounds.centerLon), 1e-9)
    }

    @Test
    fun `grid coordinates are latitude then longitude for the weather API`() {
        val first = gridCoordinates(
            centerLat = 20.0,
            centerLon = 77.0,
            spanLat = 8.0,
            spanLon = 10.0
        ).first()

        assertEquals(16.0, first.first, 1e-9) // latitude = 20 - span/2
        assertEquals(72.0, first.second, 1e-9) // longitude = 77 - span/2
    }

    @Test
    fun `grid coordinates stay valid near the poles and antimeridian`() {
        val coordinates = gridCoordinates(
            centerLat = 84.0,
            centerLon = 179.0,
            spanLat = 10.0,
            spanLon = 20.0
        )

        assertTrue(coordinates.isNotEmpty())
        coordinates.forEach { (lat, lon) ->
            assertTrue("latitude $lat must be renderable", lat in -85.0..85.0)
            assertTrue("longitude $lon must be normalised", lon in -180.0..180.0)
        }
        // Crossing 180° wraps to negative longitudes instead of exceeding 180°.
        assertTrue(coordinates.any { it.second < 0.0 })
    }

    @Test
    fun `arrow length is shorter than grid spacing`() {
        val bounds = gridBounds(
            BoundingBox(west = -0.25, south = 19.9, east = 0.25, north = 20.1)
        )
        val arrowLength = windArrowLengthDegrees(bounds.spanLat, bounds.spanLon)

        assertTrue(arrowLength > 0.0)
        assertTrue(arrowLength < min(bounds.spanLat, bounds.spanLon) / 4.0)
    }

    @Test
    fun `a real viewport-sized grid produces renderable temperature and wind data`() {
        val bounds = gridBounds(
            BoundingBox(west = -0.25, south = 19.9, east = 0.25, north = 20.1)
        )
        val coordinates = gridCoordinates(
            bounds.centerLat,
            bounds.centerLon,
            bounds.spanLat,
            bounds.spanLon
        )
        val temperatures = coordinates.mapIndexed { index, (lat, lon) ->
            TempGridPoint(lat = lat, lon = lon, temp = 20.0 + index)
        }
        val winds = coordinates.map { (lat, lon) ->
            WindPoint(lat = lat, lon = lon, speed = 10.0, direction = 90.0)
        }

        assertEquals(25, coordinates.size)
        assertEquals(25, features(temperatureOverlayData(temperatures)).size)
        assertEquals(
            25,
            features(
                windOverlayData(
                    winds,
                    windArrowLengthDegrees(bounds.spanLat, bounds.spanLon)
                )
            ).size
        )
    }

    @Test
    fun `longitude normalisation wraps into the valid range`() {
        assertEquals(-170.0, normalizeLongitude(190.0), 1e-9)
        assertEquals(170.0, normalizeLongitude(-190.0), 1e-9)
        assertEquals(180.0, normalizeLongitude(180.0), 1e-9)
        assertEquals(0.0, normalizeLongitude(360.0), 1e-9)
    }
}
