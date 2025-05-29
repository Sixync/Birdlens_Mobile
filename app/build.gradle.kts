// EXE201/app/build.gradle.kts

import java.util.Properties
import java.io.FileInputStream


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

// Read API key from local.properties (project root)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties") // Path relative to project root
var googleMapsApiKeyFromProperties = "YOUR_API_KEY_MISSING_IN_LOCAL_PROPERTIES" // Default/fallback value

if (localPropertiesFile.exists()) {
    try {
        FileInputStream(localPropertiesFile).use { fis ->
            localProperties.load(fis)
            googleMapsApiKeyFromProperties = localProperties.getProperty("MAPS_API_KEY") // Get property, could be null
            if (googleMapsApiKeyFromProperties != null) {
                println("Successfully loaded MAPS_API_KEY from local.properties.")
            } else {
                println("Warning: MAPS_API_KEY not found in local.properties.")
            }
        }
    } catch (e: Exception) {
        System.err.println("Warning: Could not load MAPS_API_KEY from local.properties: ${e.message}")
        println("Warning: Failed to load MAPS_API_KEY from local.properties.")
    }
} else {
    println("Warning: local.properties file not found. MAPS_API_KEY will not be set from it.")
}


val googleMapsApiKey = if (googleMapsApiKeyFromProperties.isNullOrBlank()) {
    println("Using default/fallback API Key because value from local.properties was null or blank.")
    "YOUR_API_KEY_MISSING_OR_BLANK" // A distinct fallback
} else {
    googleMapsApiKeyFromProperties!!
}

println("MAPS_API_KEY to be used in build: ${googleMapsApiKey.take(5)}...")


android {
    namespace = "com.android.birdlens"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.birdlens"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.android)
    // Material Icons
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    // Coil
    implementation(libs.coil.compose)
    implementation(libs.maps.compose) // Added
    implementation(libs.play.services.maps)
    implementation(libs.androidx.media3.common.ktx) // Added

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:33.13.0")) // Ensure this is a recent version
    implementation("com.google.firebase:firebase-analytics")

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Sign-In (for One Tap)
    implementation("com.google.android.gms:play-services-auth:20.7.0") // This version is okay for now


    // Retrofit for network calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson) // Or Moshi, etc.
    implementation(libs.logging.interceptor) // For logging network requests (optional)
}