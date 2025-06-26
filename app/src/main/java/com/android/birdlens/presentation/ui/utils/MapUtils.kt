// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/utils/MapUtils.kt
package com.android.birdlens.presentation.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * Creates a BitmapDescriptor from a vector drawable resource.
 * This is useful for creating custom map markers.
 *
 * @param context The application context.
 * @param vectorResId The resource ID of the vector drawable.
 * @param tintColor An optional Compose Color to tint the drawable.
 * @param sizeMultiplier A factor to scale the size of the resulting bitmap.
 * @return A BitmapDescriptor, or null if an error occurs.
 */
fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int,
    tintColor: Color? = null,
    sizeMultiplier: Float = 1.0f
): BitmapDescriptor? {
    return try {
        ContextCompat.getDrawable(context, vectorResId)?.let { vectorDrawable ->
            val mutatedDrawable = vectorDrawable.mutate()
            if (tintColor != null) {
                // Convert Compose Color to Android Color Int
                val androidColor = android.graphics.Color.argb(
                    (tintColor.alpha * 255).toInt(),
                    (tintColor.red * 255).toInt(),
                    (tintColor.green * 255).toInt(),
                    (tintColor.blue * 255).toInt()
                )
                DrawableCompat.setTint(mutatedDrawable, androidColor)
            }
            val originalWidth = mutatedDrawable.intrinsicWidth.takeIf { it > 0 } ?: 72
            val originalHeight = mutatedDrawable.intrinsicHeight.takeIf { it > 0 } ?: 72

            val width = (originalWidth * sizeMultiplier).toInt()
            val height = (originalHeight * sizeMultiplier).toInt()

            mutatedDrawable.setBounds(0, 0, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            mutatedDrawable.draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    } catch (e: NullPointerException) {
        Log.e("MapUtils", "Error creating BitmapDescriptor: IBitmapDescriptorFactory not initialized or other NPE.", e)
        null
    } catch (e: Exception) {
        Log.e("MapUtils", "Generic error creating BitmapDescriptor.", e)
        null
    }
}