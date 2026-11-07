package app.multinest.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.multinest.R
import app.multinest.data.AppDatabase
import app.multinest.data.AutoColor
import app.multinest.data.ProfileEntity
import app.multinest.data.ProfileRepository
import app.multinest.engine.ProfileEngine
import app.multinest.shortcuts.ShortcutHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ProfileRepository(AppDatabase.get(app).profileDao())

    val query = MutableStateFlow("")
    val profiles: StateFlow<List<ProfileEntity>> = query
        .flatMapLatest { repo.observe(it.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** One-shot message as a string resource id. */
    var message by mutableStateOf<Int?>(null)

    suspend fun get(id: String) = repo.get(id)

    fun update(p: ProfileEntity) {
        viewModelScope.launch { repo.update(p) }
    }

    /**
     * Creates [count] independent profiles (each gets its own UUID and its own storage).
     * [manualHue] == null means automatic, practically unlimited distinct colors.
     */
    fun createBatch(name: String, url: String, note: String, manualHue: Float?, icon: String, count: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val start = repo.count()
            repo.createMany(
                (1..count).map { i ->
                    ProfileEntity(
                        id = UUID.randomUUID().toString(),
                        name = if (count == 1) name else "$name $i",
                        url = url,
                        colorArgb = if (manualHue != null)
                            android.graphics.Color.HSVToColor(floatArrayOf((manualHue + (i - 1) * 23f) % 360f, 0.65f, 0.85f))
                        else AutoColor.forIndex(start + i),
                        iconKey = icon, lastUrl = null, favorite = false, createdAt = now + i,
                        note = note,
                    )
                }
            )
        }
    }

    fun toggleFavorite(p: ProfileEntity) {
        viewModelScope.launch { repo.update(p.copy(favorite = !p.favorite)) }
    }

    fun delete(p: ProfileEntity) {
        viewModelScope.launch {
            // Step 1: erase this profile's engine storage; refuses if a window still uses it
            if (!ProfileEngine.deleteStorage(p.id)) {
                message = R.string.delete_failed
                return@launch
            }
            // Step 2: only then remove metadata and the pinned shortcut
            repo.delete(p.id)
            ShortcutHelper.disable(getApplication(), p.id)
        }
    }

    fun createTestPair() {
        viewModelScope.launch {
            repo.createMany(
                listOf("A", "B").mapIndexed { i, tag ->
                    ProfileEntity(
                        UUID.randomUUID().toString(), "Isolation test $tag",
                        "https://httpbin.org/cookies/set?who=$tag",
                        if (i == 0) 0xFFE53935.toInt() else 0xFF1E88E5.toInt(),
                        ProfileIcons.DEFAULT, null, false, System.currentTimeMillis(),
                    )
                }
            )
        }
    }

    fun createStressProfiles(count: Int = 100) {
        viewModelScope.launch {
            repo.createMany(
                (1..count).map { i ->
                    ProfileEntity(
                        UUID.randomUUID().toString(), "Test account $i", "https://example.com",
                        AutoColor.forIndex(i),
                        ProfileIcons.DEFAULT, null, false, System.currentTimeMillis(),
                    )
                }
            )
        }
    }
}
