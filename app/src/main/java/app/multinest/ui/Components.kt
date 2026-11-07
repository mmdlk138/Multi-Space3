package app.multinest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Rounded-square avatar: initial letter or a glyph, on the profile color. */
@Composable
fun ProfileAvatar(name: String, iconKey: String, color: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).background(color, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        if (iconKey == ProfileIcons.LETTER) {
            Text(
                name.trim().take(1).uppercase().ifEmpty { "?" },
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp,
            )
        } else {
            Icon(ProfileIcons.get(iconKey), null, tint = Color.White, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** Slim pill-shaped text field used for the address bar and for search. */
@Composable
fun PillField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leading: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    ltr: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    Surface(modifier.height(44.dp), shape = RoundedCornerShape(22.dp), color = cs.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = cs.onSurface,
                        textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content,
                    ),
                    cursorBrush = SolidColor(cs.primary),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
