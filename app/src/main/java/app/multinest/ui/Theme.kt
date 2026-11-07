package app.multinest.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF4F46E5), onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF), onPrimaryContainer = Color(0xFF1E1B4B),
    background = Color(0xFFF6F6F8), onBackground = Color(0xFF16161A),
    surface = Color.White, onSurface = Color(0xFF16161A),
    surfaceVariant = Color(0xFFEDEEF2), onSurfaceVariant = Color(0xFF666A75),
    errorContainer = Color(0xFFFDE2E1), onErrorContainer = Color(0xFF5C1A17),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF8B93FF), onPrimary = Color(0xFF14163A),
    primaryContainer = Color(0xFF2E3170), onPrimaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF101013), onBackground = Color(0xFFEDEDF0),
    surface = Color(0xFF1A1A1F), onSurface = Color(0xFFEDEDF0),
    surfaceVariant = Color(0xFF26262C), onSurfaceVariant = Color(0xFFA0A3AE),
    errorContainer = Color(0xFF5C1A17), onErrorContainer = Color(0xFFFDE2E1),
)

/** Flat, minimal theme: one accent color, no dynamic color, no shadows. */
@Composable
fun MultiNestTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
    }
}
