package app.multinest.engine

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/** One Gecko runtime per app process. Each profile gets a persistent, isolated session context. */
object ProfileEngine {
    @Volatile private var runtime: GeckoRuntime? = null

    @Synchronized fun runtime(context: Context): GeckoRuntime {
        runtime?.let { return it }
        return GeckoRuntime.create(context.applicationContext, GeckoRuntimeSettings.Builder().build()).also {
            runtime = it
            it.warmUp()
        }
    }

    fun isSupported(): Boolean = true

    /** Stable context ID derived only from the profile UUID, never from its editable name. */
    fun storageName(profileId: String): String = "multispace_" + profileId.replace('-', '_')

    fun deleteStorage(profileId: String): Boolean {
        val r = runtime ?: return true
        return runCatching { r.storageController.clearDataForSessionContext(storageName(profileId)); true }.getOrDefault(false)
    }

    fun flush(profileId: String) { /* Gecko persists session-context data automatically. */ }
}
