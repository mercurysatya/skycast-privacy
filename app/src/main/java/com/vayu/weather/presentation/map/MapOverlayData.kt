package com.vayu.weather.presentation.map

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.expressions.ast.Expression
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.step
import org.maplibre.compose.expressions.value.ColorValue
import org.maplibre.compose.expressions.value.FloatValue
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import kotlin.math.cos
import kotlin.math.sin

/**
 * Number of grid samples per axis used by the temperature/wind overlays.
 * The grid is deliberately small: the overlays are a coarse visual field,
 * and every sample is one Open-Meteo request.
 */
internal const val MAP_GRID_SAMPLES_PER_AXIS = 5

/** Latitude limit accepted by the weather API and the map renderer. */
private const val MAX_ABS_LATITUDE = 85.0

/** Coldest colour used below the first temperature stop. */
internal val TemperatureColdestColor = Color(0xFF1A237E)

/**
 * Temperature bands mirroring the legend. Each stop is the lower bound of
 * the band, so `-10` means "blue from -10 up to the next stop".
 */
internal val TemperatureColorStops: List<Pair<Float, Color>> = listOf(
    -10f to Color(0xFF1565C0),
    0f to Color(0xFF0097A7),
    5f to Color(0xFF2E7D32),
    10f to Color(0xFF689F38),
    15f to Color(0xFFFDD835),
    20f to Color(0xFFFF8F00),
    25f to Color(0xFFF44336),
    30f to Color(0xFFB71C1C),
    35f to Color(0xFF4A148C)
)

/** Calm colour used below the first wind-speed stop. */
internal val WindCalmColor = Color(0xFF81C784)

/** Wind-speed bands mirroring the legend. */
internal val WindColorStops: List<Pair<Float, Color>> = listOf(
    5f to Color(0xFFFFD54F),
    15f to Color(0xFFFF8A65),
    25f to Color(0xFFE53935),
    35f to Color(0xFF6A1B9A)
)

/** Colour for a temperature in degrees Celsius, matching the legend. */
internal fun temperatureColor(temp: Double): Color =
    TemperatureColorStops.lastOrNull { temp >= it.first }?.second ?: TemperatureColdestColor

/** Colour for a wind speed in km/h, matching the legend. */
internal fun windSpeedColor(speed: Double): Color =
    WindColorStops.lastOrNull { speed >= it.first }?.second ?: WindCalmColor

/** A `step` expression that paints a value with the same bands as the legend. */
private fun colorStepExpression(
    input: Expression<FloatValue>,
    fallback: Color,
    stops: List<Pair<Float, Color>>
): Expression<ColorValue> {
    val stopArgs = stops.map { (value, color) ->
        Pair<Number, Expression<ColorValue>>(value, const(color))
    }.toTypedArray()
    return step<ColorValue>(input, const(fallback), *stopArgs)
}

internal fun temperatureColorExpression(): Expression<ColorValue> =
    colorStepExpression(feature["temp"].asNumber(), TemperatureColdestColor, TemperatureColorStops)

internal fun windColorExpression(): Expression<ColorValue> =
    colorStepExpression(feature["speed"].asNumber(), WindCalmColor, WindColorStops)

/**
 * Builds the point features rendered by the temperature circle layer.
 *
 * GeoJSON layers avoid the screen-projection problem that caused the previous
 * canvas implementation to be removed: MapLibre positions the points itself.
 */
internal fun temperatureOverlayData(points: List<TempGridPoint>): GeoJsonData =
    GeoJsonData.Features(
        FeatureCollection(
            points.map { point ->
                Feature(
                    geometry = Point(
                        Position(longitude = point.lon, latitude = point.lat)
                    ),
                    properties = buildJsonObject { put("temp", point.temp) }
                )
            }
        )
    )

/**
 * Builds one arrow per wind sample. The arrow points where the wind is going
 * (meteorological direction is where it comes from).
 */
internal fun windOverlayData(
    points: List<WindPoint>,
    arrowLengthDegrees: Double
): GeoJsonData =
    GeoJsonData.Features(
        FeatureCollection(
            points.map { point ->
                Feature(
                    geometry = LineString(windArrowPositions(point, arrowLengthDegrees)),
                    properties = buildJsonObject { put("speed", point.speed) }
                )
            }
        )
    )

/**
 * Shaft + arrowhead for one wind sample. The retrace through the tip keeps it
 * a single LineString, which MapLibre renders without needing a second layer.
 */
internal fun windArrowPositions(point: WindPoint, arrowLengthDegrees: Double): List<Position> {
    val length = arrowLengthDegrees.coerceAtLeast(0.0001)
    // Wind direction is where the air comes FROM; the arrow points the other way.
    val forward = Math.toRadians((point.direction + 180.0) % 360.0)
    val start = Position(
        longitude = normalizeLongitude(point.lon),
        latitude = point.lat.coerceIn(-MAX_ABS_LATITUDE, MAX_ABS_LATITUDE)
    )
    val tip = offsetPosition(start, forward, length)
    val headLength = length * 0.38
    val left = offsetPosition(tip, forward + Math.toRadians(150.0), headLength)
    val right = offsetPosition(tip, forward - Math.toRadians(150.0), headLength)
    return listOf(start, tip, left, tip, right)
}

/** Grid centre and span covering the map viewport. */
internal data class GridBounds(
    val centerLat: Double,
    val centerLon: Double,
    val spanLat: Double,
    val spanLon: Double
)

/**
 * Derives the grid from the map's visible bounding box. This is more reliable
 * than guessing the viewport from the zoom level: the map composable is
 * measured in dp and MapLibre's tile scale differs from the 256px assumption,
 * so a zoom-only formula can cover several screens and leave most samples
 * off-screen.
 */
internal fun gridBounds(bbox: BoundingBox): GridBounds {
    val south = bbox.south.coerceIn(-MAX_ABS_LATITUDE, MAX_ABS_LATITUDE)
    val north = bbox.north.coerceIn(-MAX_ABS_LATITUDE, MAX_ABS_LATITUDE)
    val spanLat = (north - south).coerceIn(0.005, 90.0)
    var lonSpan = bbox.east - bbox.west
    if (lonSpan < 0) lonSpan += 360.0
    val spanLon = lonSpan.coerceIn(0.005, 120.0)
    return GridBounds(
        centerLat = ((south + north) / 2).coerceIn(-MAX_ABS_LATITUDE, MAX_ABS_LATITUDE),
        centerLon = normalizeLongitude(bbox.west + lonSpan / 2),
        spanLat = spanLat,
        spanLon = spanLon
    )
}

/** Grid sample coordinates, clamped to valid latitudes and wrapped longitudes. */
internal fun gridCoordinates(
    centerLat: Double,
    centerLon: Double,
    spanLat: Double,
    spanLon: Double
): List<Pair<Double, Double>> {
    val steps = MAP_GRID_SAMPLES_PER_AXIS - 1
    val half = steps / 2
    val latStep = spanLat / steps
    val lonStep = spanLon / steps
    return (-half..half).flatMap { latIndex ->
        (-half..half).mapNotNull { lonIndex ->
            val lat = centerLat + latIndex * latStep
            if (lat < -MAX_ABS_LATITUDE || lat > MAX_ABS_LATITUDE) {
                null
            } else {
                lat to normalizeLongitude(centerLon + lonIndex * lonStep)
            }
        }
    }
}

/** Arrow length that keeps neighbouring arrows from touching. */
internal fun windArrowLengthDegrees(spanLat: Double, spanLon: Double): Double =
    (minOf(spanLat, spanLon) / 10.0).coerceAtLeast(0.001)

internal fun normalizeLongitude(longitude: Double): Double {
    var normalized = longitude % 360.0
    if (normalized > 180.0) normalized -= 360.0
    if (normalized < -180.0) normalized += 360.0
    return normalized
}

private fun offsetPosition(from: Position, bearingRadians: Double, distanceDegrees: Double): Position {
    val latitudeRadians = Math.toRadians(from.latitude)
    val deltaLat = distanceDegrees * cos(bearingRadians)
    val deltaLon = distanceDegrees * sin(bearingRadians) /
        cos(latitudeRadians).coerceAtLeast(0.05)
    return Position(
        longitude = normalizeLongitude(from.longitude + deltaLon),
        latitude = (from.latitude + deltaLat).coerceIn(-MAX_ABS_LATITUDE, MAX_ABS_LATITUDE)
    )
}
