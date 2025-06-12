// EXE201/app/src/main/java/com/android/birdlens/MyApp.kt
package com.android.birdlens

import android.app.Application
import com.stripe.android.PaymentConfiguration

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51RYfcnJjb7tLdjg9B5I6SuiRqNJsEbuEG3dqmeiImEm4ID5xt4JWaqk2FFEwOPs9cWGJPOpOGCykE5dyS2HZRbcd00QNiusvIa"
        )
    }
}