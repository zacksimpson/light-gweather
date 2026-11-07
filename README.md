# GWeather

A weather tool for the Light Phone III that uses Google's Weather API instead of Open-Meteo.

It's a fork of the weather example in the [Light SDK](https://github.com/lightphone/light-sdk). The forecast data comes from Google now, and everything else works the way the original does. Open-Meteo wasn't accurate enough for where I live, so this was my way around that.

> [!NOTE]
> Not affiliated with Google or Light.

---

## Bring your own API key

This tool needs your own Google Maps Platform API key. The key gets built into the app, so there's no prebuilt APK to download (sorry!). You'll build it yourself, which only takes a minute once the key is set up.

1. Create a Google Cloud project, turn on billing, and enable the **Weather API**.
2. Create an API key and restrict it to the Weather API only.
3. Copy `local.properties.example` to `local.properties` (same folder as `build.gradle.kts`) and paste your key after the `=`:

```
googleWeatherApiKey=YOUR_KEY
```

`local.properties` is gitignored, so it stays on your machine. Please don't commit it!

If you skip this step the app still builds, it just shows "No Google Weather API key" instead of a forecast.

> [!WARNING]
> Your key is compiled into the APK, so anyone who has the app can pull it out. Restrict it to the Weather API, and set a daily quota cap and a budget alert on your Google Cloud project before you use it. Google gives you a free monthly allowance but bills for calls past that, so check their [pricing](https://developers.google.com/maps/billing-and-pricing/pricing) first.

Each refresh makes three calls (current, daily and hourly). To go easy on your quota, the app reuses the saved forecast for 15 minutes instead of refetching every time you open it.

---

## Local Setup

<details>
<summary>Building from source</summary>

This module isn't a standalone Gradle build. It compiles as a subproject of a local `light-sdk` checkout, the same way other Light SDK tools do.

Clone this repo next to the SDK and add this to the bottom of the SDK's `settings.gradle.kts` (local-only, do not commit):

```kotlin
include(":gweather")
project(":gweather").projectDir = file("../light-gweather")
```

Then, from inside the `light-sdk` checkout:

```
./gradlew :gweather:installDebug
```

</details>

---

## Good to know

* Location search still goes through Light's proxy, which uses Open-Meteo's geocoding, so Settings credits both Google and Open-Meteo.
* Google asks for attribution and limits how long you can store their data. Read the [Weather API policies](https://developers.google.com/maps/documentation/weather/policies) before you share a build with anyone.
* The tool ID in `lighttool.toml` is `com.zacksimpson.gweather`. Change it (and the imports that go with it) if you want your own.

## Support

If any of my tools have been useful to you, I'd love to hear from you! Feel free to [open an issue](https://github.com/zacksimpson/light-gweather/issues), or [consider sponsoring](https://github.com/sponsors/zacksimpson). Either way, it means a lot!

## Credits

* [The Light Phone](https://www.thelightphone.com) - for building a phone worth making tools for
* [Google Maps Platform](https://developers.google.com/maps/documentation/weather/overview) - for the Weather API
* [Open-Meteo](https://open-meteo.com) - for location search, via Light's proxy

## License

[MIT](LICENSE), same as the Light SDK.
