# LightOS Weather tool with Google Weather as the backend

A weather tool for the Light Phone III that uses Google's Weather API instead of Open-Meteo.

This is a fork of the open-source Weather tool in the [Light SDK](https://github.com/lightphone/light-sdk). The forecast data comes from Google, and everything else works the way the original does. Open-Meteo wasn't accurate enough for where I live, so this was my way around that.

Not affiliated with Google or Light.

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

New to Google Cloud? There's a [step-by-step guide](docs/google-cloud-setup.md) that walks through the project, billing, key, and spending limits.

If you skip this step the app still builds, it just shows "No Google Weather API key" instead of a forecast.

> [!WARNING]
> Your key is compiled into the APK. Restrict it to the Weather API, and I'd suggest setting a daily quota cap and a budget alert on your Google Cloud project before you use it. Google gives you a free monthly allowance but bills for calls past that, so check their [pricing](https://developers.google.com/maps/billing-and-pricing/pricing) first.

Each refresh makes three calls (current, daily and hourly). To minimize chances of hitting your limits, the app reuses the saved forecast for 15 minutes instead of refetching every time you open it.

---

## Local Setup

<details>
<summary>Building from source</summary>

This module compiles as a subproject of a local `light-sdk` checkout (like a typical Light SDK tool).

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
* Read the [Weather API policies](https://developers.google.com/maps/documentation/weather/policies) before you share a build with anyone.
* The tool ID in `lighttool.toml` is `com.zacksimpson.gweather`. Change it (and the imports that go with it) if you want your own.

## Support

If any of my tools have been useful to you, I'd love to hear from you! Feel free to [open an issue](https://github.com/zacksimpson/light-gweather/issues), or [consider sponsoring](https://github.com/sponsors/zacksimpson). Either way, it means a lot!

## Credits

* [The Light Phone](https://www.thelightphone.com) - for building a phone worth making tools for
* [Google Maps Platform](https://developers.google.com/maps/documentation/weather/overview) - for the Weather API
* [Open-Meteo](https://open-meteo.com) - for location search, via Light's proxy
