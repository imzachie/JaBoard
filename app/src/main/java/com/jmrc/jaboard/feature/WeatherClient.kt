package com.jmrc.jaboard.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import com.jmrc.jaboard.data.WeatherCache
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

 data class CityResult(val name: String, val admin: String, val country: String, val latitude: Double, val longitude: Double) {
    val label: String get() = listOf(name, admin, country).filter(String::isNotBlank).distinct().joinToString(", ")
}
data class WeatherReport(
    val temperature: Double, val feelsLike: Double, val humidity: Int, val wind: Double,
    val rainChance: Int?, val code: Int, val hourly: List<Pair<String, Double>>, val daily: List<DayForecast>, val updatedAt: Long = System.currentTimeMillis()
)
data class DayForecast(val date: String, val high: Double, val low: Double, val code: Int, val rain: Int?)

object WeatherClient {
    suspend fun searchCity(query: String): List<CityResult> = withContext(Dispatchers.IO) {
        require(query.trim().length >= 2) { "Enter at least two letters" }
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val root = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=8&language=en&format=json"))
        val results = root.optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { i -> results.getJSONObject(i) }.map {
            CityResult(it.optString("name"), it.optString("admin1"), it.optString("country"), it.getDouble("latitude"), it.getDouble("longitude"))
        }
    }

    suspend fun forecast(latitude: Double, longitude: Double): WeatherReport = withContext(Dispatchers.IO) {
        val query = "latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,weather_code&hourly=temperature_2m,precipitation_probability&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&forecast_days=7&timezone=auto"
        val root = JSONObject(get("https://api.open-meteo.com/v1/forecast?$query"))
        val current = root.getJSONObject("current")
        val hourlyRoot = root.optJSONObject("hourly")
        val hourlyTimes = hourlyRoot?.optJSONArray("time")
        val hourlyTemperatures = hourlyRoot?.optJSONArray("temperature_2m")
        val hourly = if (hourlyTimes != null && hourlyTemperatures != null) (0 until minOf(hourlyTimes.length(), hourlyTemperatures.length(), 24)).map { i ->
            hourlyTimes.getString(i).substringAfter('T') to hourlyTemperatures.getDouble(i)
        } else emptyList()
        val dayRoot = root.optJSONObject("daily")
        val dates = dayRoot?.optJSONArray("time")
        val highs = dayRoot?.optJSONArray("temperature_2m_max")
        val lows = dayRoot?.optJSONArray("temperature_2m_min")
        val codes = dayRoot?.optJSONArray("weather_code")
        val rains = dayRoot?.optJSONArray("precipitation_probability_max")
        val days = if (dates != null && highs != null && lows != null && codes != null) (0 until dates.length()).map { i ->
            DayForecast(dates.getString(i), highs.getDouble(i), lows.getDouble(i), codes.getInt(i), rains?.optInt(i))
        } else emptyList()
        WeatherReport(
            temperature = current.getDouble("temperature_2m"),
            feelsLike = current.getDouble("apparent_temperature"),
            humidity = current.getInt("relative_humidity_2m"),
            wind = current.getDouble("wind_speed_10m"),
            rainChance = hourlyRoot?.optJSONArray("precipitation_probability")?.optInt(0),
            code = current.getInt("weather_code"), hourly = hourly, daily = days
        )
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 12_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "JaBoard/1.0 (Android utility)")
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            check(code in 200..299) { "Weather service returned HTTP $code" }
            body
        } finally { connection.disconnect() }
    }
}

fun WeatherReport.toCache(city: String, latitude: Double, longitude: Double): WeatherCache {
    val data = JSONObject()
        .put("temperature", temperature).put("feelsLike", feelsLike).put("humidity", humidity)
        .put("wind", wind).put("rainChance", rainChance ?: JSONObject.NULL).put("code", code)
        .put("hourly", JSONArray().also { array -> hourly.forEach { array.put(JSONArray().put(it.first).put(it.second)) } })
        .put("daily", JSONArray().also { array -> daily.forEach { array.put(JSONObject().put("date", it.date).put("high", it.high).put("low", it.low).put("code", it.code).put("rain", it.rain ?: JSONObject.NULL)) } })
    return WeatherCache(city = city, latitude = latitude, longitude = longitude, payload = data.toString(), fetchedAt = updatedAt)
}

fun WeatherCache.toReport(): WeatherReport {
    val data = JSONObject(payload)
    val hours = data.optJSONArray("hourly")
    val days = data.optJSONArray("daily")
    return WeatherReport(
        temperature = data.getDouble("temperature"), feelsLike = data.getDouble("feelsLike"), humidity = data.getInt("humidity"),
        wind = data.getDouble("wind"), rainChance = data.takeIf { !it.isNull("rainChance") }?.optInt("rainChance"), code = data.getInt("code"),
        hourly = if (hours == null) emptyList() else (0 until hours.length()).map { i -> val row = hours.getJSONArray(i); row.getString(0) to row.getDouble(1) },
        daily = if (days == null) emptyList() else (0 until days.length()).map { i -> val row = days.getJSONObject(i); DayForecast(row.getString("date"), row.getDouble("high"), row.getDouble("low"), row.getInt("code"), row.takeIf { !it.isNull("rain") }?.optInt("rain")) },
        updatedAt = fetchedAt
    )
}

fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mostly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61, 63, 65 -> "Rain"
    66, 67 -> "Freezing rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Conditions unavailable"
}
