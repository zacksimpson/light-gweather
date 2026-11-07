import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.light.sdk)
}

android {
    compileSdk = rootProject.ext["compileSdk"] as Int

    defaultConfig {
        minSdk = rootProject.ext["minSdk"] as Int
        targetSdk = rootProject.ext["targetSdk"] as Int

        manifestPlaceholders["sdkVersion"] = property("sdkVersion") as String

        // your own google weather api key. it goes in local.properties next to this file
        // (gitignored). ~/.gradle/gradle.properties and the GOOGLE_WEATHER_API_KEY env var also work.
        val localProps = Properties().apply {
            val f = project.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        val weatherKey = localProps.getProperty("googleWeatherApiKey")?.trim()
            ?: providers.gradleProperty("googleWeatherApiKey").orNull
            ?: providers.environmentVariable("GOOGLE_WEATHER_API_KEY").orNull
            ?: ""
        buildConfigField("String", "GOOGLE_WEATHER_API_KEY", "\"$weatherKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("lightsdkDev") {
            storeFile = rootProject.file("sdk/keys/lightsdk-dev.jks")
            storePassword = "android"
            keyAlias = "lightsdk-dev"
            keyPassword = "android"
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("lightsdkDev")
        }
        release {
            signingConfig = signingConfigs.getByName("lightsdkDev")
        }
    }

    lint {
        warningsAsErrors = false
        error += "RestrictedApi"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(rootProject.ext["jvmTarget"] as String)
        targetCompatibility = JavaVersion.toVersion(rootProject.ext["jvmTarget"] as String)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(rootProject.ext["jvmTarget"] as String))
    }
}

dependencies {
    implementation(project(":sdk:client"))
    implementation(libs.kotlinx.datetime)
    testImplementation(libs.kotlin.test)
}
