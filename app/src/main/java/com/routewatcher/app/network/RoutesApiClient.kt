package com.routewatcher.app.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

// One selectable road option returned by computeRoutes with alternatives

data class RouteOption(

    // Generic label ("Route 1")
    // Routes API has no short route-name field like the old Directions API's "summary"

    val summary: String,
    val distanceText: String,
    val durationMinutes: Int,
    val encodedPolyline: String,
    val waypoints: List<Pair<Double, Double>>, // pinned points for future checks
)

// TODO: fetchRouteAlternatives is called by RoutePickerScreen

object RoutesApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://routes.googleapis.com/directions/v2:computeRoutes"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun fetchRouteAlternatives(origin: String, destination: String, apiKey: String): List<RouteOption> {
        if (apiKey.isBlank()) return emptyList()

        val body = JSONObject().apply {
            put("origin", JSONObject().put("address", origin))
            put("destination", JSONObject().put("address", destination))
            put("travelMode", "DRIVE")
            put("routingPreference", "TRAFFIC_AWARE")
            put("computeAlternativeRoutes", true)
            put("departureTime", Instant.now().toString())
        }

        val request = buildRequest(
            body,
            fieldMask = "routes.duration,routes.staticDuration,routes.distanceMeters,routes.polyline.encodedPolyline",
            apiKey = apiKey,
        )

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: return emptyList()
                parseAlternatives(responseBody)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun buildRequest(body: JSONObject, fieldMask: String, apiKey: String): Request =
        Request.Builder()
            .url(BASE_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", fieldMask)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

    private fun parseAlternatives(responseBody: String): List<RouteOption> {
        val json = JSONObject(responseBody)
        val routes = json.optJSONArray("routes") ?: return emptyList()
        val result = mutableListOf<RouteOption>()

        for (i in 0 until routes.length()) {
            val route = routes.getJSONObject(i)
            val distanceMeters = route.optInt("distanceMeters", 0)
            val durationSeconds = parseSecondsField(route.optString("duration", "0s"))
            val polyline = route.optJSONObject("polyline")?.optString("encodedPolyline") ?: continue

            result.add(
                RouteOption(
                    summary = "Route ${i + 1}",
                    distanceText = "%.1f km".format(distanceMeters / 1000.0),
                    durationMinutes = durationSeconds / 60,
                    encodedPolyline = polyline,
                    waypoints = derivePinningWaypoints(polyline),
                ),
            )
        }
        return result
    }

    // Routes API returns durations as strings like "1371s", not the
    // {"value": N} object Distance Matrix used. (This pulls out the number)
    // Pure function, no framework dependency (unit tested)
    internal fun parseSecondsField(raw: String): Int =
        raw.removeSuffix("s").toIntOrNull() ?: 0

    private fun derivePinningWaypoints(encodedPolyline: String): List<Pair<Double, Double>> {
        val decoded = decodePolyline(encodedPolyline)
        if (decoded.size < 2) return emptyList()
        val fractions = listOf(0.25, 0.5, 0.75)
        return fractions.map { fraction ->
            val index = (decoded.size * fraction).toInt().coerceIn(0, decoded.size - 1)
            decoded[index]
        }
    }

    // Standard Google encoded-polyline decoder.
    // Pure function, no framework dependency (unit tested)
    internal fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        val poly = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            poly.add(Pair(lat / 1E5, lng / 1E5))
        }
        return poly
    }
}