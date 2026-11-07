package com.thelightphone.weather

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

private const val WEATHER_API_BASE = "https://production.lightphonecloud.com"

@Serializable
internal data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList(),
)

@Serializable
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
)

internal class WeatherApi {
    private val json = Json { ignoreUnknownKeys = true }

    private val googleWeather = GoogleWeatherApi()

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun searchLocations(query: String): Result<List<GeocodingResult>> = runCatching {
        val encoded = URLEncoder.encode(query.trim(), UTF_8.name())
        val response = client.get(
            "$WEATHER_API_BASE/tools/weather/geolocation?name=$encoded&count=15",
        )

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText().take(500)
            throw IllegalStateException("Geocoding HTTP ${response.status.value}: $body")
        }

        val geo: GeocodingResponse = response.body()
        geo.results.ifEmpty { throw LocationNotFoundException() }
    }

    suspend fun fetchForecast(latitude: Double, longitude: Double): Result<StoredForecast> =
        googleWeather.fetchForecast(latitude, longitude)

    fun close() {
        googleWeather.close()
        client.close()
    }
}

internal class LocationNotFoundException : Exception("Location not found.")

fun GeocodingResult.displayName(): String {
    val region = regionLabel()
    return if (region.isEmpty()) name else "$name, $region"
}

fun GeocodingResult.regionLabel(): String =
    listOfNotNull(admin1, country).joinToString(", ")
