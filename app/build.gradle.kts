import java.util.Properties
import java.io.FileInputStream


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    id("com.google.gms.google-services")
}

// Read API key from local.properties (project root)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties") // Path relative to project root
var googleMapsApiKeyFromProperties = "YOUR_API_KEY_MISSING_IN_LOCAL_PROPERTIES" // Default/fallback value
var ebirdApiKeyFromProperties = "YOUR_EBIRD_API_KEY_MISSING" // Default/fallback for eBird


if (localPropertiesFile.exists()) {
    try {
        FileInputStream(localPropertiesFile).use { fis ->
            localProperties.load(fis)
            googleMapsApiKeyFromProperties = localProperties.getProperty("MAPS_API_KEY")
            ebirdApiKeyFromProperties = localProperties.getProperty("EBIRD_API_KEY") // Get eBird API key

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
    "YOUR_EBIRD_API_KEY_MISSING_IN_CONFIG" // A distinct fallback
} else {
    ebirdApiKeyFromProperties!!
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
        resourceConfigurations.addAll(listOf("en", "vi"))
        buildConfigField("String", "EBIRD_API_KEY", "\"$ebirdApiKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true // This line enables the BuildConfig class generation
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
    // buildFeatures { // Already defined above, removing duplicate
    //     compose = true
    // }
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
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.core.ktx) // Added

    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

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
    //implementation("com.google.android.gms:play-services-auth:20.7.0") // This version is okay for now


    // Retrofit for network calls
    implementation(libs.retrofit)
    implementation(libs.converter.gson) // Or Moshi, etc.
    implementation(libs.logging.interceptor) // For logging network requests (optional)

    // Google Mobile Ads SDK (AdMob)
    implementation("com.google.android.gms:play-services-ads:23.2.0") // Check for the latest version

    // Room Persistence Library
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler) // For Kotlin annotation processing
    implementation(libs.androidx.room.ktx)
}