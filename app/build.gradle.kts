import java.util.Properties
import java.io.FileInputStream


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
var googleMapsApiKeyFromProperties = "YOUR_API_KEY_MISSING_IN_LOCAL_PROPERTIES"
var ebirdApiKeyFromProperties = "YOUR_EBIRD_API_KEY_MISSING"
// Logic: Remove the variable that holds the Gemini API key. It will no longer be read from local.properties for the client.
// var geminiApiKeyFromProperties = "YOUR_GEMINI_API_KEY_MISSING"
var stripePublishableKeyFromProperties: String? = null
var backendBaseUrlFromProperties: String? = null

if (localPropertiesFile.exists()) {
    try {
        FileInputStream(localPropertiesFile).use { fis ->
            localProperties.load(fis)
            googleMapsApiKeyFromProperties = localProperties.getProperty("MAPS_API_KEY")
            ebirdApiKeyFromProperties = localProperties.getProperty("EBIRD_API_KEY")
            // Logic: No longer need to read the Gemini key here. The backend will handle it.
            // geminiApiKeyFromProperties = localProperties.getProperty("GEMINI_API_KEY")
            stripePublishableKeyFromProperties = localProperties.getProperty("STRIPE_PUBLISHABLE_KEY")
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
            // Logic: Remove the print statements related to the Gemini key.
            // if (geminiApiKeyFromProperties != null) {
            //     println("Successfully loaded GEMINI_API_KEY from local.properties.")
            // } else {
            //     println("Warning: GEMINI_API_KEY not found in local.properties.")
            // }

            if (stripePublishableKeyFromProperties != null) {
                println("Successfully loaded STRIPE_PUBLISHABLE_KEY from local.properties. Value: '${stripePublishableKeyFromProperties}'")
            } else {
                println("Warning: STRIPE_PUBLISHABLE_KEY not found or is null in local.properties.")
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
// Logic: Remove the logic for handling the Gemini key.
// val geminiApiKey = if (geminiApiKeyFromProperties.isNullOrBlank() || geminiApiKeyFromProperties == "YOUR_GEMINI_API_KEY_MISSING") {
//     println("Using default/fallback Gemini API Key because value from local.properties was null, blank, or placeholder.")
//     "YOUR_GEMINI_API_KEY_MISSING_IN_CONFIG"
// } else {
//     geminiApiKeyFromProperties!!
// }

val googleMapsApiKey = if (googleMapsApiKeyFromProperties.isNullOrBlank()) {
    println("Using default/fallback API Key because value from local.properties was null or blank.")
    "YOUR_API_KEY_MISSING_OR_BLANK"
} else {
    googleMapsApiKeyFromProperties!!
}

val stripePublishableKey = if (stripePublishableKeyFromProperties.isNullOrBlank()) {
    println("Stripe Key from properties was null or blank. Using default/fallback Stripe Publishable Key: 'pk_test_DEFAULT_FALLBACK_KEY'")
    "pk_test_DEFAULT_FALLBACK_KEY"
} else {
    println("Stripe Key successfully read from properties. Using value: '${stripePublishableKeyFromProperties}'")
    stripePublishableKeyFromProperties!!
}

val backendBaseUrl = if (backendBaseUrlFromProperties.isNullOrBlank()) {
    println("Using default/fallback Backend URL: 'http://10.0.2.2/'")
    "http://10.0.2.2/"
} else {
    println("Using Backend URL from local.properties: '$backendBaseUrlFromProperties'")
    backendBaseUrlFromProperties!!
}

println("MAPS_API_KEY to be used in build: ${googleMapsApiKey.take(5)}...")
println("FINAL STRIPE_PUBLISHABLE_KEY to be injected into BuildConfig: '${stripePublishableKey.take(8)}...'")


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
        // Logic: REMOVE the buildConfigField for the Gemini API key. This is the critical change to prevent the leak.
        // buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"$stripePublishableKey\"")
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
            // Logic: This is where your production URL is defined. When you build a release APK,
            // the value of BuildConfig.BACKEND_BASE_URL will be https://birdlens.duckdns.org/
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
    // Logic: The Gemini SDK is no longer needed on the client, so we can remove this dependency.
    // implementation(libs.generativeai)
    implementation(libs.androidx.compose.material)

    // Google Maps Compose Utilities (Clustering, Heatmaps, etc.)
    implementation("com.google.maps.android:maps-compose-utils:4.3.3") // Updated to latest
    implementation("com.google.maps.android:maps-compose-widgets:4.3.3") // For ScaleBar, etc. - Updated to latest
    implementation ("com.google.maps.android:android-maps-utils:3.8.2") // For HeatmapTileProvider

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

    // Stripe SDK Dependencies
    implementation("com.stripe:stripe-android:21.17.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(libs.androidx.appcompat)
}