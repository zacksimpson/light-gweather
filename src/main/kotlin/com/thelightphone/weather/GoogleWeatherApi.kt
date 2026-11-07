package com.thelightphone.weather

import com.zacksimpson.gweather.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

private const val GOOGLE_WEATHER_BASE = "https://weather.googleapis.com/v1"

// each fetch is 3 calls (current, days, hours) and the project has a daily cap,
// so keep requests to one page each. hours max out at 24 per page.
private const val DAYS = 7
private const val HOURS = 24

@Serializable
internal data class GwTimeZone(val id: String? = null)

@Serializable
internal data class GwCondition(val description: GwDescription? = null)

@Serializable
internal data class GwDescription(val text: String? = null)

@Serializable
internal data class GwTemp(val degrees: Double, val unit: String? = null)

@Serializable
internal data class GwPrecip(val probability: GwProbability? = null, val qpf: GwQuantity? = null)

@Serializable
internal data class GwProbability(val percent: Int? = null)

@Serializable
internal data class GwQuantity(val quantity: Double = 0.0)

@Serializable
internal data class GwWind(val direction: GwDirection? = null, val speed: GwSpeed? = null)

@Serializable
internal data class GwDirection(val degrees: Int? = null)

@Serializable
internal data class GwSpeed(val value: Double = 0.0, val unit: String? = null)

@Serializable
internal data class GwCurrent(
    val weatherCondition: GwCondition? = null,
    val temperature: GwTemp? = null,
    val feelsLikeTemperature: GwTemp? = null,
)

@Serializable
internal data class GwDate(val year: Int, val month: Int, val day: Int)

@Serializable
internal data class GwSun(val sunriseTime: String? = null, val sunsetTime: String? = null)

@Serializable
internal data class GwDayPart(
    val weatherCondition: GwCondition? = null,
    val precipitation: GwPrecip? = null,
    val wind: GwWind? = null,
    val uvIndex: Int? = null,
)

@Serializable
internal data class GwDay(
    val displayDate: GwDate,
    val daytimeForecast: GwDayPart? = null,
    val nighttimeForecast: GwDayPart? = null,
    val maxTemperature: GwTemp,
    val minTemperature: GwTemp,
    val feelsLikeMaxTemperature: GwTemp? = null,
    val feelsLikeMinTemperature: GwTemp? = null,
    val sunEvents: GwSun? = null,
)

@Serializable
internal data class GwDaysResponse(
    val forecastDays: List<GwDay> = emptyList(),
    val timeZone: GwTimeZone? = null,
)

@Serializable
internal data class GwDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hours: Int = 0,
    val minutes: Int = 0,
)

@Serializable
internal data class GwHour(
    val displayDateTime: GwDateTime,
    val temperature: GwTemp,
    val feelsLikeTemperature: GwTemp? = null,
    val precipitation: GwPrecip? = null,
)

@Serializable
internal data class GwHoursResponse(val forecastHours: List<GwHour> = emptyList())

internal class GoogleWeatherApi(
    private val apiKey: String = BuildConfig.GOOGLE_WEATHER_API_KEY,
) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private suspend inline fun <reified T> get(path: String, params: String): T {
        if (apiKey.isBlank()) {
            throw IllegalStateException("No Google Weather API key. See the readme.")
        }
        val response = client.get("$GOOGLE_WEATHER_BASE/$path?key=$apiKey&$params")
        if (!response.status.isSuccess()) {
            if (response.status.value == 429) {
                throw IllegalStateException("Daily weather limit reached. Try again tomorrow.")
            }
            val body = response.bodyAsText().take(300)
            throw IllegalStateException("Weather HTTP ${response.status.value}: $body")
        }
        return response.body()
    }

    suspend fun fetchForecast(latitude: Double, longitude: Double): Result<StoredForecast> =
        runCatching {
            val loc = "location.latitude=$latitude&location.longitude=$longitude"
            coroutineScope {
                val current = async { get<GwCurrent>("currentConditions:lookup", loc) }
                val days = async {
                    get<GwDaysResponse>("forecast/days:lookup", "$loc&days=$DAYS&pageSize=$DAYS")
                }
                val hours = async {
                    get<GwHoursResponse>("forecast/hours:lookup", "$loc&hours=$HOURS&pageSize=$HOURS")
                }
                toStoredForecast(current.await(), days.await(), hours.await())
            }
        }

    fun close() {
        client.close()
    }
}

private fun GwTemp.celsius(): Double = when (unit) {
    "FAHRENHEIT" -> (degrees - 32.0) * 5.0 / 9.0
    else -> degrees
}

private fun GwSpeed.kmh(): Double = when (unit) {
    "MILES_PER_HOUR" -> value * 1.609344
    else -> value
}

// google sends fractional seconds, the ui only needs the minute
private fun String.toLocal(tz: TimeZone): LocalDateTime {
    val t = Instant.parse(this).toLocalDateTime(tz)
    return LocalDateTime(t.year, t.month, t.day, t.hour, t.minute)
}

private fun GwCondition?.text(): String? = this?.description?.text

internal fun toStoredForecast(
    current: GwCurrent,
    days: GwDaysResponse,
    hours: GwHoursResponse,
): StoredForecast {
    if (days.forecastDays.size < 2) {
        throw IllegalStateException("Forecast did not include today and tomorrow.")
    }
    val tz = days.timeZone?.id?.let { runCatching { TimeZone.of(it) }.getOrNull() }
        ?: TimeZone.currentSystemDefault()

    val dayForecasts = days.forecastDays.map { it.toDayForecast(tz) }
    return StoredForecast(
        today = dayForecasts[0],
        tomorrow = dayForecasts[1],
        weekly = dayForecasts.map { it.toWeeklyDay() },
        hourly = hours.forecastHours.map { it.toHourlyForecast() },
        current = current.temperature?.let {
            CurrentConditions(
                tempC = it.celsius(),
                apparentTempC = current.feelsLikeTemperature?.celsius(),
                weatherCode = 0,
                conditionText = current.weatherCondition.text(),
            )
        },
        daily = dayForecasts,
    )
}

private fun GwDay.toDayForecast(tz: TimeZone): DayForecast {
    val parts = listOfNotNull(daytimeForecast, nighttimeForecast)
    val winds = parts.mapNotNull { it.wind }.filter { it.speed != null }
    val windiest = winds.maxByOrNull { it.speed!!.kmh() }

    return DayForecast(
        date = LocalDate(displayDate.year, displayDate.month, displayDate.day),
        tempMaxC = maxTemperature.celsius(),
        tempMinC = minTemperature.celsius(),
        apparentTempMaxC = (feelsLikeMaxTemperature ?: maxTemperature).celsius(),
        apparentTempMinC = (feelsLikeMinTemperature ?: minTemperature).celsius(),
        precipitationMm = parts.sumOf { it.precipitation?.qpf?.quantity ?: 0.0 },
        precipitationProbabilityMax = parts.mapNotNull { it.precipitation?.probability?.percent }.maxOrNull(),
        weatherCode = 0,
        conditionText = (daytimeForecast ?: nighttimeForecast)?.weatherCondition.text(),
        windSpeedMaxKmh = windiest?.speed?.kmh() ?: 0.0,
        windDirectionDominant = windiest?.direction?.degrees ?: 0,
        uvIndexMax = (daytimeForecast?.uvIndex ?: 0).toDouble(),
        sunrise = sunEvents?.sunriseTime?.toLocal(tz),
        sunset = sunEvents?.sunsetTime?.toLocal(tz),
    )
}

private fun DayForecast.toWeeklyDay() = WeeklyDay(
    date = date,
    tempMaxC = tempMaxC,
    tempMinC = tempMinC,
    precipitationMm = precipitationMm,
    precipitationProbabilityMax = precipitationProbabilityMax,
    weatherCode = weatherCode,
    conditionText = conditionText,
)

private fun GwHour.toHourlyForecast() = HourlyForecast(
    time = LocalDateTime(
        displayDateTime.year,
        displayDateTime.month,
        displayDateTime.day,
        displayDateTime.hours,
        displayDateTime.minutes,
    ),
    tempC = temperature.celsius(),
    apparentTempC = (feelsLikeTemperature ?: temperature).celsius(),
    precipitationMm = precipitation?.qpf?.quantity ?: 0.0,
    precipitationProbability = precipitation?.probability?.percent,
)
