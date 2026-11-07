# light-gweather

a weather tool for the light phone iii that uses the google maps platform weather api instead of open-meteo. it is a fork of the weather example in [light-sdk](https://github.com/lightphone/light-sdk).

not affiliated with google or light.

## what you need

- the light sdk, checked out locally
- a google cloud project with the weather api enabled, billing on, and an api key

## setup

1. clone this repo next to the sdk and include it from the sdk's `settings.gradle.kts`:

   ```kotlin
   include(":gweather")
   project(":gweather").projectDir = file("../light-gweather")
   ```

2. add your api key. copy `local.properties.example` to `local.properties` (same folder as `build.gradle.kts`) and paste your key after the `=`:

   ```
   googleWeatherApiKey=YOUR_KEY
   ```

   `local.properties` is gitignored so it stays on your machine. never commit it.

3. build and install:

   ```
   ./gradlew :gweather:installDebug
   ```

without a key the app still builds, but it shows "No Google Weather API key" instead of a forecast.

## about your api key

- the key is compiled into the apk, so anyone with the apk can pull it out. restrict it to the weather api only.
- google bills per call after a free monthly allowance (check their current pricing). set a daily quota cap and a budget alert on your project before you use it.
- each refresh makes three calls (current, daily, hourly). the app reuses a saved forecast for 15 minutes so it does not refetch every time you open it.

## notes

- location search still goes through light's proxy, which uses open-meteo geocoding, so the app credits both sources in settings.
- google's terms ask for attribution and limit how long you can store its data. read the [weather api policies](https://developers.google.com/maps/documentation/weather/policies) before you distribute a build.
- the tool id in `lighttool.toml` is `com.zacksimpson.gweather`. change it (and the matching imports) if you want your own.

## license

mit, same as light-sdk. see `LICENSE`.
