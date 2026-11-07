package app.multinest.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata only. The browsing data (cookies, storage) lives inside the WebView
 * profile whose name is derived from [id] — never from [name] or [colorArgb].
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,          // persistent UUID = storage identity
    val name: String,
    val url: String,
    val colorArgb: Int,
    val iconKey: String,
    val lastUrl: String?,
    val favorite: Boolean,
    val createdAt: Long,
    /** Free text to tell accounts apart, e.g. the e-mail you log in with. */
    @ColumnInfo(defaultValue = "''") val note: String = "",
)
