package com.thelightphone.weather

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// fixtures follow the shape of real weather.googleapis.com responses (la, utc-7)
private fun part(text: String, pop: Int, qpf: Double, wind: Int, dir: Int, uv: Int) = """
  {"weatherCondition":{"description":{"text":"$text","languageCode":"en"},"type":"X"},
   "precipitation":{"probability":{"percent":$pop,"type":"RAIN"},"qpf":{"quantity":$qpf,"unit":"MILLIMETERS"}},
   "wind":{"direction":{"degrees":$dir,"cardinal":"WEST"},"speed":{"value":$wind,"unit":"KILOMETERS_PER_HOUR"}},
   "uvIndex":$uv}
""".trimIndent()

private fun day(d: Int, hi: Double, lo: Double, feelHi: Double, feelLo: Double) = """
  {"displayDate":{"year":2026,"month":7,"day":$d},
   "daytimeForecast":${part("Partly sunny", 10, 0.5, 20, 283, 3)},
   "nighttimeForecast":${part("Clear", 5, 1.0, 30, 250, 0)},
   "maxTemperature":{"degrees":$hi,"unit":"CELSIUS"},
   "minTemperature":{"degrees":$lo,"unit":"CELSIUS"},
   "feelsLikeMaxTemperature":{"degrees":$feelHi,"unit":"CELSIUS"},
   "feelsLikeMinTemperature":{"degrees":$feelLo,"unit":"CELSIUS"},
   "sunEvents":{"sunriseTime":"2026-07-${d}T13:10:00.5Z","sunsetTime":"2026-07-${d + 1}T03:22:00Z"}}
""".trimIndent()

private val DAYS_JSON = """
{"forecastDays":[${day(28, 23.4, 12.7, 24.5, 11.9)},${day(29, 21.0, 13.0, 21.3, 13.3)},${day(30, 25.0, 14.0, 26.0, 13.0)}],
 "timeZone":{"id":"America/Los_Angeles","version":""}}
""".trimIndent()

private val HOURS_JSON = run {
    val hours = (0..23).joinToString(",") { h ->
        """{"displayDateTime":{"year":2026,"month":7,"day":28,"hours":$h,"minutes":0,"seconds":0,"nanos":0,"utcOffset":"-25200s"},
            "temperature":{"degrees":${14 + h * 0.5},"unit":"CELSIUS"},
            "feelsLikeTemperature":{"degrees":${13 + h * 0.5},"unit":"CELSIUS"},
            "precipitation":{"probability":{"percent":${h % 5},"type":"RAIN"},"qpf":{"quantity":0.0,"unit":"MILLIMETERS"}}}"""
    }
    """{"forecastHours":[$hours],"timeZone":{"id":"America/Los_Angeles"}}"""
}

private val CURRENT_JSON = """
{"weatherCondition":{"description":{"text":"Mostly clear","languageCode":"en"},"type":"MOSTLY_CLEAR"},
 "temperature":{"degrees":18.4,"unit":"CELSIUS"},
 "feelsLikeTemperature":{"degrees":17.9,"unit":"CELSIUS"},
 "timeZone":{"id":"America/Los_Angeles"}}
""".trimIndent()

private val json = Json { ignoreUnknownKeys = true }

private fun build(
    currentJson: String = CURRENT_JSON,
    daysJson: String = DAYS_JSON,
): StoredForecast = toStoredForecast(
    json.decodeFromString<GwCurrent>(currentJson),
    json.decodeFromString<GwDaysResponse>(daysJson),
    json.decodeFromString<GwHoursResponse>(HOURS_JSON),
)

private fun sampleForecast() = build()

class WeatherModelsTest {

    @Test
    fun `maps display dates, hours and utc sun times into local time`() {
        val f = sampleForecast()
        assertEquals(LocalDate.parse("2026-07-28"), f.today.date)
        assertEquals(LocalDateTime.parse("2026-07-28T06:10"), f.today.sunrise)
        assertEquals(LocalDateTime.parse("2026-07-28T20:22"), f.today.sunset)
        assertEquals(24, f.hourly.size)
        assertEquals(LocalDateTime.parse("2026-07-28T00:00"), f.hourly.first().time)
    }

    @Test
    fun `day fields combine daytime and nighttime parts`() {
        val today = sampleForecast().today
        assertEquals(23.4, today.tempMaxC)
        assertEquals(12.7, today.tempMinC)
        assertEquals(24.5, today.apparentTempMaxC)
        assertEquals(11.9, today.apparentTempMinC)
        assertEquals(1.5, today.precipitationMm)
        assertEquals(10, today.precipitationProbabilityMax)
        assertEquals(3.0, today.uvIndexMax)
        // night wind is stronger, so it wins along with its direction
        assertEquals(30.0, today.windSpeedMaxKmh)
        assertEquals(250, today.windDirectionDominant)
        assertEquals("W", today.windCompass)
    }

    @Test
    fun `google condition text is used directly`() {
        val f = sampleForecast()
        assertEquals("Mostly clear", f.current?.weatherDescription)
        assertEquals("Partly sunny", f.today.weatherDescription)
        assertEquals("Partly sunny", f.weekly[1].weatherDescription)
    }

    @Test
    fun `one day of forecast fails`() {
        val oneDay = """{"forecastDays":[${day(28, 20.0, 10.0, 20.0, 10.0)}],"timeZone":{"id":"America/Los_Angeles"}}"""
        assertFailsWith<IllegalStateException> { build(daysJson = oneDay) }
    }

    @Test
    fun `fahrenheit and mph responses are converted to metric`() {
        val current = """{"weatherCondition":{"description":{"text":"Hot"}},"temperature":{"degrees":212.0,"unit":"FAHRENHEIT"}}"""
        assertEquals(100.0, build(currentJson = current).current?.tempC)

        val mph = DAYS_JSON.replace("KILOMETERS_PER_HOUR", "MILES_PER_HOUR")
        val windy = build(daysJson = mph).today.windSpeedMaxKmh
        assertEquals(30 * 1.609344, windy, 0.001)
    }

    @Test
    fun `dayCount prefers weekly size, then daily size, then falls back to two`() {
        val forecast = sampleForecast() // weekly and daily both have 3 days

        val weeklyLonger = forecast.copy(weekly = forecast.weekly + forecast.weekly[0])
        assertEquals(4, weeklyLonger.dayCount())

        assertEquals(3, forecast.copy(weekly = emptyList()).dayCount())
        assertEquals(2, forecast.copy(weekly = emptyList(), daily = emptyList()).dayCount())
    }

    @Test
    fun `dayAt reads straight from the daily list when present`() {
        val forecast = sampleForecast()
        assertEquals(forecast.daily[2], forecast.dayAt(2))
        assertNull(forecast.dayAt(3))
    }

    @Test
    fun `dayAt falls back to today, tomorrow, and converted weekly days`() {
        val forecast = sampleForecast().copy(daily = emptyList())

        assertEquals(forecast.today, forecast.dayAt(0))
        assertEquals(forecast.tomorrow, forecast.dayAt(1))

        val thirdDay = forecast.dayAt(2)
        assertNotNull(thirdDay)
        assertEquals(LocalDate.parse("2026-07-30"), thirdDay.date)
        assertEquals("Partly sunny", thirdDay.weatherDescription)
        // WeeklyDay carries no wind/UV/sunrise data
        assertNull(thirdDay.sunrise)
        assertNull(thirdDay.sunset)
        assertEquals(0, thirdDay.windDirectionDominant)
    }

    @Test
    fun `dayAt returns null out of range`() {
        val forecast = sampleForecast().copy(daily = emptyList(), weekly = emptyList())
        assertNull(forecast.dayAt(2))
        assertNull(forecast.dayAt(-1))
    }

    @Test
    fun `hoursForToday keeps only todays hours at or after the current hour`() {
        val hours = sampleForecast().hoursForToday(now = LocalDateTime.parse("2026-07-28T10:30"))

        assertEquals(14, hours.size) // 10:00 through 23:00
        assertEquals(LocalDateTime.parse("2026-07-28T10:00"), hours.first().time)
        assertEquals(LocalDateTime.parse("2026-07-28T23:00"), hours.last().time)
        assertTrue(hours.all { it.time.date == LocalDate.parse("2026-07-28") })
    }

    @Test
    fun `hoursForToday truncates now to the top of the hour`() {
        val hours = sampleForecast().hoursForToday(now = LocalDateTime.parse("2026-07-28T12:45"))

        assertEquals(12, hours.size) // 12:00 through 23:00
        assertEquals(LocalDateTime.parse("2026-07-28T12:00"), hours.first().time)
    }

    @Test
    fun `missing condition text falls back to the old wmo table`() {
        val noText = CurrentConditions(tempC = 10.0, weatherCode = 0)
        assertEquals("Clear sky", noText.weatherDescription)
        assertEquals("Thunderstorm with hail", wmoWeatherDescription(96))
        assertEquals("Weather code 42", wmoWeatherDescription(42))
    }

    @Test
    fun `missing api key fails clearly without making a request`() {
        val result = kotlinx.coroutines.runBlocking {
            GoogleWeatherApi(apiKey = "").fetchForecast(0.0, 0.0)
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API key") == true)
    }

    @Test
    fun `units default to imperial unless metric was saved`() {
        assertEquals(TemperatureUnit.Fahrenheit, temperatureUnitFromStorage(null))
        assertEquals(TemperatureUnit.Fahrenheit, temperatureUnitFromStorage("F"))
        assertEquals(TemperatureUnit.Celsius, temperatureUnitFromStorage("C"))
        assertEquals("72°", formatTemperature(22.2, TemperatureUnit.Fahrenheit))
        assertEquals("5 mph NW", formatWindSpeed(8.0, "NW", TemperatureUnit.Fahrenheit))
        assertEquals("0.1 in", formatRain(2.54, TemperatureUnit.Fahrenheit))
    }

    @Test
    fun `wind compass reads direction off the day forecast`() {
        assertEquals("N", degreesToCompass(0))
        assertEquals("N", degreesToCompass(359))
        assertEquals("W", degreesToCompass(283))
    }
}
