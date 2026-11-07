# light-gweather

A weather tool for the Light Phone III that uses the Google Maps Platform Weather API instead of Open-Meteo. It is a fork of the weather example in [light-sdk](https://github.com/lightphone/light-sdk).

Not affiliated with Google or Light.

## What you need

- The Light SDK, checked out locally
- A Google Cloud project with the Weather API enabled, billing on, and an API key

## Setup

1. Clone this repo next to the SDK and include it from the SDK's `settings.gradle.kts`:

   ```kotlin
   include(":gweather")
   project(":gweather").projectDir = file("../light-gweather")
   ```

2. Add your API key. Copy `local.properties.example` to `local.properties` (same folder as `build.gradle.kts`) and paste your key after the `=`:

   ```
   googleWeatherApiKey=YOUR_KEY
   ```

   `local.properties` is gitignored so it stays on your machine. Never commit it.

3. Build and install:

   ```
   ./gradlew :gweather:installDebug
   ```

Without a key the app still builds, but it shows "No Google Weather API key" instead of a forecast.

## About your API key

- The key is compiled into the APK, so anyone with the APK can pull it out. Restrict it to the Weather API only.
- Google bills per call after a free monthly allowance (check their current pricing). Set a daily quota cap and a budget alert on your project before you use it.
- Each refresh makes three calls (current, daily, hourly). The app reuses a saved forecast for 15 minutes so it does not refetch every time you open it.

## Notes

- Location search still goes through Light's proxy, which uses Open-Meteo geocoding, so the app credits both sources in Settings.
- Google's terms ask for attribution and limit how long you can store its data. Read the [Weather API policies](https://developers.google.com/maps/documentation/weather/policies) before you distribute a build.
- The tool id in `lighttool.toml` is `com.zacksimpson.gweather`. Change it (and the matching imports) if you want your own.

## License

MIT, same as light-sdk. See `LICENSE`.
