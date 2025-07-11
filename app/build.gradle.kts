// path: EXE201/app/build.gradle.kts
import java.util.Properties
import java.io.FileInputStream


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
var googleMapsApiKeyFromProperties = "YOUR_API_KEY_MISSING_IN_LOCAL_PROPERTIES"
var ebirdApiKeyFromProperties = "YOUR_EBIRD_API_KEY_MISSING"
// Logic: The reference to the Stripe publishable key is removed.
var backendBaseUrlFromProperties: String? = null

if (localPropertiesFile.exists()) {
    try {
        FileInputStream(localPropertiesFile).use { fis ->
            localProperties.load(fis)
            googleMapsApiKeyFromProperties = localProperties.getProperty("MAPS_API_KEY")
            ebirdApiKeyFromProperties = localProperties.getProperty("EBIRD_API_KEY")
            // Logic: The loading of STRIPE_PUBLISHABLE_KEY is removed.
            backendBaseUrlFromProperties = localProperties.getProperty("BACKEND_BASE_URL_LOCAL")


            if (googleMapsApiKeyFromProperties != null) {
                println("Successfully loaded MAPS_API_KEY from local.properties.")
            } else {
                println("Warning: MAPS_API_KEY not found in local.properties.")
            }
            if (ebirdApiKeyFromProperties != null) {
                println("Successfully loaded EBIRD_API_KEY from local.properties.")
            } else {
                println("Warning: EBIRD_API_KEY not found in local.properties.")
            }

        }
    } catch (e: Exception) {
        System.err.println("Warning: Could not load API keys from local.properties: ${e.message}")
        println("Warning: Failed to load API keys from local.properties.")
    }
} else {
    println("Warning: local.properties file not found. API keys will not be set from it.")
}

val ebirdApiKey = if (ebirdApiKeyFromProperties.isNullOrBlank() || ebirdApiKeyFromProperties == "YOUR_EBIRD_API_KEY_MISSING") {
    println("Using default/fallback eBird API Key because value from local.properties was null, blank, or placeholder.")
    "YOUR_EBIRD_API_KEY_MISSING_IN_CONFIG"
} else {
    ebirdApiKeyFromProperties!!
}

val googleMapsApiKey = if (googleMapsApiKeyFromProperties.isNullOrBlank()) {
    println("Using default/fallback API Key because value from local.properties was null or blank.")
    "YOUR_API_KEY_MISSING_OR_BLANK"
} else {
    googleMapsApiKeyFromProperties!!
}

// Logic: The stripePublishableKey variable and its related logic are removed.

val backendBaseUrl = if (backendBaseUrlFromProperties.isNullOrBlank()) {
    println("Using default/fallback Backend URL: 'http://10.0.2.2/'")
    "http://10.0.2.2/"
} else {
    println("Using Backend URL from local.properties: '$backendBaseUrlFromProperties'")
    backendBaseUrlFromProperties!!
}

println("MAPS_API_KEY to be used in build: ${googleMapsApiKey.take(5)}...")
// Logic: The log for the final Stripe key is removed.


android {
    namespace = "com.android.birdlens"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.birdlens"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsApiKey
        resourceConfigurations.addAll(listOf("en", "vi"))
        buildConfigField("String", "EBIRD_API_KEY", "\"$ebirdApiKey\"")
        // Logic: The BuildConfig field for STRIPE_PUBLISHABLE_KEY is removed.
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BACKEND_BASE_URL", "\"https://birdlens.duckdns.org/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    kapt {
        javacOptions {
            option("-source", JavaVersion.VERSION_11.toString())
            option("-target", JavaVersion.VERSION_11.toString())
        }
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
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.core.ktx)

    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("com.google.accompanist:accompanist-webview:0.34.0")
    implementation(libs.androidx.compose.material)

    implementation("com.google.maps.android:maps-compose-utils:4.3.3")
    implementation("com.google.maps.android:maps-compose-widgets:4.3.3")
    implementation ("com.google.maps.android:android-maps-utils:3.8.2")
    implementation(libs.play.services.location)

    // Logic: Add the Jetpack Media3 Transformer dependency to handle video conversion.
    implementation("androidx.media3:media3-transformer:1.3.1")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.firebase:firebase-auth-ktx")


    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    implementation("com.google.android.gms:play-services-ads:23.2.0")

    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.foundation:foundation")

    // Logic: The Stripe SDK dependencies are removed from the project.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    implementation(libs.androidx.appcompat)
}