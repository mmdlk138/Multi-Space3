package app.multinest.data

import android.graphics.Color

/**
 * Automatic, practically unlimited distinct colors: successive hues are 137.5 degrees apart
 * (golden angle), so neighbours never look alike, and saturation / brightness cycle through
 * bands so even hundreds of profiles stay distinguishable. Color is only a hint;
 * the name and note are what identify an account, and the UUID is the storage identity.
 */
object AutoColor {
    private val saturation = floatArrayOf(0.65f, 0.50f, 0.80f)
    private val value = floatArrayOf(0.85f, 0.70f, 0.95f)

    fun forIndex(n: Int): Int {
        val hue = (n * 137.508f) % 360f
        val band = (n / 12) % saturation.size
        return Color.HSVToColor(floatArrayOf(hue, saturation[band], value[band]))
    }
}
