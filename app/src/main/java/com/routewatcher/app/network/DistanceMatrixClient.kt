package com.routewatcher.app.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TrafficResult(
    val success: Boolean,
    val normalDurationMinutes: Int = 0,
    val trafficDurationMinutes: Int = 0,
    val delayMinutes: Int = 0,
    val errorMessage: String? = null,
)

// Asks Google for "the route between A and B"
object DistanceMatrixClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun checkTraffic(origin: String, destination: String, apiKey: String): TrafficResult {
        if (apiKey.isBlank()) return TrafficResult(success = false, errorMessage = "No API key set")

        val url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                "?origins=${enc(origin)}&destinations=${enc(destination)}" +
                "&departure_time=now&traffic_model=best_guess&key=${enc(apiKey)}"

        val request = Request.Builder().url(url).build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                    ?: return TrafficResult(false, errorMessage = "Empty response")
                parse(body)
            }
        } catch (e: Exception) {
            TrafficResult(success = false, errorMessage = e.message ?: "Network error")
        }
    }

    // Extracts normal vs. traffic-adjusted duration from Google's response shape.
    private fun parse(body: String): TrafficResult {
        val json = JSONObject(body)
        if (json.optString("status") != "OK") {
            return TrafficResult(false, errorMessage = "API status: ${json.optString("status")}")
        }
        val element = json.getJSONArray("rows").getJSONObject(0)
            .getJSONArray("elements").getJSONObject(0)

        if (element.optString("status") != "OK") {
            return TrafficResult(false, errorMessage = "Route status: ${element.optString("status")}")
        }

        val normalSeconds = element.getJSONObject("duration").getInt("value")
        val trafficSeconds = element.optJSONObject("duration_in_traffic")?.getInt("value") ?: normalSeconds
        val normalMin = normalSeconds / 60
        val trafficMin = trafficSeconds / 60

        return TrafficResult(
            success = true,
            normalDurationMinutes = normalMin,
            trafficDurationMinutes = trafficMin,
            delayMinutes = (trafficMin - normalMin).coerceAtLeast(0),
        )
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}