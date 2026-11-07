package app.multinest.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.multinest.R
import app.multinest.data.ProfileEntity
import app.multinest.data.ProfileRepository
import app.multinest.engine.ProfileEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

/** One open tab per profile. Gecko context IDs keep cookies and site storage separate. */
class BrowserTab(initial: ProfileEntity) {
    var profile by mutableStateOf(initial)
    var address by mutableStateOf(initial.lastUrl ?: initial.url)
    var progress by mutableIntStateOf(100)
    var canBack by mutableStateOf(false)
    var canForward by mutableStateOf(false)
    var desktop by mutableStateOf(false)
    var session: GeckoSession? = null
    var browserView: GeckoView? = null
    var restoreUrl: String = initial.lastUrl ?: initial.url
    val id: String get() = profile.id
}

class TabManager(private val repo: ProfileRepository, private val scope: CoroutineScope) {
    val tabs = mutableStateListOf<BrowserTab>()
    var activeId by mutableStateOf<String?>(null); private set
    var activityResumed = true
    var browserVisible = false
    private val liveOrder = ArrayList<String>()
    val active: BrowserTab? get() = tabs.firstOrNull { it.id == activeId }
    fun isOpen(id: String) = tabs.any { it.id == id }
    fun open(p: ProfileEntity) { if (tabs.none { it.id == p.id }) tabs.add(BrowserTab(p)); activeId = p.id }
    fun select(id: String) { activeId = id; syncLifecycle() }
    fun updateProfile(p: ProfileEntity) { tabs.firstOrNull { it.id == p.id }?.profile = p }
    fun setStartUrl(tab: BrowserTab, url: String) { tab.profile = tab.profile.copy(url = url); scope.launch { repo.setStartUrl(tab.id, url) } }
    fun close(id: String) {
        val idx = tabs.indexOfFirst { it.id == id }; if (idx < 0) return
        destroySession(tabs[idx]); tabs.removeAt(idx)
        if (activeId == id) activeId = tabs.getOrNull(minOf(idx, tabs.lastIndex))?.id
        syncLifecycle()
    }
    fun destroyAll() { tabs.toList().forEach { destroySession(it) } }
    fun flushAll() { tabs.forEach { ProfileEngine.flush(it.id) } }
    fun syncLifecycle() {
        val visible = activityResumed && browserVisible
        tabs.forEach { t -> t.session?.setActive(visible && t.id == activeId) }
    }
    fun browserViewFor(tab: BrowserTab, ctx: Context): GeckoView? {
        tab.browserView?.let { touch(tab.id); return it }
        return runCatching { createBrowserView(tab, ctx) }.onFailure { Log.e(TAG, "Gecko init failed for ${tab.id}", it) }.getOrNull()?.also {
            tab.browserView = it; touch(tab.id); evict(); syncLifecycle()
        }
    }
    private fun touch(id: String) { liveOrder.remove(id); liveOrder.add(id) }
    private fun evict() { while (liveOrder.size > MAX_LIVE) {
        val victim = liveOrder.firstOrNull { it != activeId } ?: break
        tabs.firstOrNull { it.id == victim }?.let { destroySession(it) } ?: liveOrder.remove(victim)
    } }
    private fun destroySession(tab: BrowserTab) {
        val session = tab.session
        if (session != null) {
            runCatching { session.stop() }
            runCatching { session.setActive(false); session.close() }
        }
        tab.browserView?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
        tab.session = null; tab.browserView = null; liveOrder.remove(tab.id)
    }
    private fun createBrowserView(tab: BrowserTab, ctx: Context): GeckoView {
        val settings = GeckoSessionSettings.Builder()
            .contextId(ProfileEngine.storageName(tab.id))
            .allowJavascript(true)
            .useTrackingProtection(false)
            .suspendMediaWhenInactive(false)
            .userAgentMode(if (tab.desktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(if (tab.desktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .build()
        val session = GeckoSession(settings)
        session.setContentDelegate(object : GeckoSession.ContentDelegate {})
        session.setProgressDelegate(object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) { tab.address = url; tab.progress = 0 }
            override fun onProgressChange(session: GeckoSession, progress: Int) { tab.progress = progress }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                tab.progress = 100
                if (tab.address.startsWith("http://") || tab.address.startsWith("https://")) {
                    tab.restoreUrl = tab.address
                    scope.launch { repo.setLastUrl(tab.id, tab.address) }
                }
                Log.d(TAG, "Gecko page stopped [${tab.id}] success=$success url=${tab.address}")
            }
        })
        session.setNavigationDelegate(object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) { tab.canBack = canGoBack }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) { tab.canForward = canGoForward }
            override fun onLocationChange(session: GeckoSession, url: String?, perms: List<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) {
                if (url != null) tab.address = url
            }
        })
        session.open(ProfileEngine.runtime(ctx))
        val view = GeckoView(ctx)
        view.setSession(session)
        tab.session = session
        session.loadUri(tab.restoreUrl)
        return view
    }
    fun toggleDesktop(tab: BrowserTab) {
        tab.desktop = !tab.desktop
        tab.session?.let { session ->
            val settings = session.settings
            settings.userAgentMode = if (tab.desktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            settings.viewportMode = if (tab.desktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
            session.loadUri(tab.address)
        }
    }
    companion object { private const val TAG = "MultiSpaceGecko"; const val MAX_LIVE = 4 }
}
