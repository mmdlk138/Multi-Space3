package app.multinest.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

object ProfileIcons {
    const val LETTER = "letter"
    const val DEFAULT = LETTER
    val all: Map<String, ImageVector> = linkedMapOf(
        "person" to Icons.Filled.Person,
        "email" to Icons.Filled.Email,
        "star" to Icons.Filled.Star,
        "heart" to Icons.Filled.Favorite,
        "home" to Icons.Filled.Home,
        "cart" to Icons.Filled.ShoppingCart,
        "send" to Icons.Filled.Send,
        "lock" to Icons.Filled.Lock,
        "phone" to Icons.Filled.Phone,
        "place" to Icons.Filled.Place,
    )
    fun get(key: String): ImageVector = all[key] ?: Icons.Filled.Person
}
