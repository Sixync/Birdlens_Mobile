
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
var geminiApiKeyFromProperties = "YOUR_GEMINI_API_KEY_MISSING"
var stripePublishableKeyFromProperties: String? = null // Initialize as nullable String

if (localPropertiesFile.exists()) {
    try {
        FileInputStream(localPropertiesFile).use { fis ->
            localProperties.load(fis)
            googleMapsApiKeyFromProperties = localProperties.getProperty("MAPS_API_KEY")
            ebirdApiKeyFromProperties = localProperties.getProperty("EBIRD_API_KEY")
            geminiApiKeyFromProperties = localProperties.getProperty("GEMINI_API_KEY")
            stripePublishableKeyFromProperties = localProperties.getProperty("STRIPE_PUBLISHABLE_KEY") // Reading the key

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
            if (geminiApiKeyFromProperties != null) {
                println("Successfully loaded GEMINI_API_KEY from local.properties.")
            } else {
                println("Warning: GEMINI_API_KEY not found in local.properties.")
            }

            // More specific check for Stripe key
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
val geminiApiKey = if (geminiApiKeyFromProperties.isNullOrBlank() || geminiApiKeyFromProperties == "YOUR_GEMINI_API_KEY_MISSING") {
    println("Using default/fallback Gemini API Key because value from local.properties was null, blank, or placeholder.")
    "YOUR_GEMINI_API_KEY_MISSING_IN_CONFIG"
} else {
    geminiApiKeyFromProperties!!
}

val googleMapsApiKey = if (googleMapsApiKeyFromProperties.isNullOrBlank()) {
    println("Using default/fallback API Key because value from local.properties was null or blank.")
    "YOUR_API_KEY_MISSING_OR_BLANK"
} else {
    googleMapsApiKeyFromProperties!!
}

// Read Stripe Publishable Key
val stripePublishableKey = if (stripePublishableKeyFromProperties.isNullOrBlank()) { // Simplified condition: if null or blank from properties file
    println("Stripe Key from properties was null or blank. Using default/fallback Stripe Publishable Key: 'pk_test_DEFAULT_FALLBACK_KEY'")
    "pk_test_DEFAULT_FALLBACK_KEY"
} else {
    println("Stripe Key successfully read from properties. Using value: '${stripePublishableKeyFromProperties}'")
    stripePublishableKeyFromProperties!!
}


println("MAPS_API_KEY to be used in build: ${googleMapsApiKey.take(5)}...")
println("FINAL STRIPE_PUBLISHABLE_KEY to be injected into BuildConfig: '${stripePublishableKey.take(8)}...'") // Debugging line


android {
    namespace = "com.android.birdlens"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.birdlens"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsApiKey
        resourceConfigurations.addAll(listOf("en", "vi"))
        buildConfigField("String", "EBIRD_API_KEY", "\"$ebirdApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"$stripePublishableKey\"")
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
    implementation(libs.generativeai)
    implementation(libs.androidx.compose.material)

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