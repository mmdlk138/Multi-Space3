package app.multinest.shortcuts

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.multinest.data.ProfileEntity
import app.multinest.ui.MainActivity

object ShortcutHelper {
    const val SCHEME = "multinest"

    /** The one and only way to open a profile: data = multinest://profile/<uuid>. */
    fun launchIntent(context: Context, profileId: String): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("$SCHEME://profile/$profileId"))

    fun profileIdFrom(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme != SCHEME || data.host != "profile") return null
        return data.lastPathSegment?.takeIf { it.isNotBlank() }
    }

    /** Asks the launcher to pin a shortcut. The launcher shows its own confirmation. */
    fun requestPin(context: Context, p: ProfileEntity): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val info = ShortcutInfoCompat.Builder(context, "profile-${p.id}")
            .setShortLabel(p.name.take(24))
            .setLongLabel(p.name)
            .setIcon(IconCompat.createWithAdaptiveBitmap(iconBitmap(p)))
            .setIntent(launchIntent(context, p.id))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    fun disable(context: Context, profileId: String) {
        ShortcutManagerCompat.disableShortcuts(context, listOf("profile-$profileId"), "Profile deleted")
    }

    private fun iconBitmap(p: ProfileEntity): Bitmap {
        val size = 432
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(p.colorArgb)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = size * 0.34f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val letter = p.name.trim().take(1).uppercase().ifEmpty { "?" }
        val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
        c.drawText(letter, size / 2f, y, paint)
        return bmp
    }
}
