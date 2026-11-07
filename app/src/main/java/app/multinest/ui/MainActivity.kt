package app.multinest.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.multinest.R
import app.multinest.data.AppDatabase
import app.multinest.data.AutoColor
import app.multinest.data.ProfileEntity
import app.multinest.data.ProfileRepository
import app.multinest.data.UrlUtils
import app.multinest.engine.ProfileEngine
import app.multinest.shortcuts.ShortcutHelper
import kotlinx.coroutines.launch
import java.net.URI

/**
 * Single activity. Profiles open as in-app tabs, so you can switch between 100 logged-in
 * accounts without leaving the app. Pinned shortcuts (singleTask) open / switch to that profile's tab.
 */
class MainActivity : ComponentActivity() {
    private val repo by lazy { ProfileRepository(AppDatabase.get(this).profileDao()) }
    private lateinit var tabs: TabManager
    private val browserRequest = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabs = TabManager(repo, lifecycleScope)
        setContent { MultiNestTheme { App(tabs, browserRequest) } }
        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val id = ShortcutHelper.profileIdFrom(i) ?: return
        lifecycleScope.launch {
            val p = repo.get(id)
            when {
                p == null -> Toast.makeText(this@MainActivity, R.string.not_found, Toast.LENGTH_LONG).show()
                !ProfileEngine.isSupported() -> Toast.makeText(this@MainActivity, R.string.unsupported, Toast.LENGTH_LONG).show()
                else -> { tabs.open(p); browserRequest.value = true }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        tabs.activityResumed = false
        tabs.flushAll()
        tabs.syncLifecycle()
    }

    override fun onResume() {
        super.onResume()
        tabs.activityResumed = true
        tabs.syncLifecycle()
    }

    override fun onDestroy() {
        tabs.destroyAll()
        super.onDestroy()
    }
}

private enum class Screen { LIST, EDIT, SETTINGS, BROWSER }

fun homepagePref(ctx: Context): String =
    ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("homepage", "https://duckduckgo.com")!!

fun setHomepagePref(ctx: Context, value: String) {
    ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putString("homepage", value).apply()
}

private fun hostOf(url: String): String = runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: url

@Composable
private fun App(tabs: TabManager, browserRequest: MutableState<Boolean>, vm: ProfileViewModel = viewModel()) {
    val ctx = LocalContext.current
    var screen by rememberSaveable { mutableStateOf(Screen.LIST) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(vm.message) {
        vm.message?.let { Toast.makeText(ctx, ctx.getString(it), Toast.LENGTH_LONG).show(); vm.message = null }
    }
    LaunchedEffect(browserRequest.value) {
        if (browserRequest.value) { screen = Screen.BROWSER; browserRequest.value = false }
    }
    LaunchedEffect(screen, tabs.activeId, tabs.tabs.size) {
        tabs.browserVisible = screen == Screen.BROWSER
        tabs.syncLifecycle()
    }
    BackHandler(enabled = screen != Screen.LIST) { screen = Screen.LIST }

    fun openProfile(p: ProfileEntity) {
        if (ProfileEngine.isSupported()) { tabs.open(p); screen = Screen.BROWSER }
        else Toast.makeText(ctx, R.string.unsupported, Toast.LENGTH_LONG).show()
    }

    when (screen) {
        Screen.LIST -> ProfileListScreen(
            vm, tabs,
            onOpen = ::openProfile,
            onAdd = { editId = null; screen = Screen.EDIT },
            onEdit = { editId = it.id; screen = Screen.EDIT },
            onSettings = { screen = Screen.SETTINGS },
            onBrowser = { screen = Screen.BROWSER },
            onDelete = { p -> tabs.close(p.id); vm.delete(p) },
        )
        Screen.EDIT -> {
            val id = editId
            val loaded by produceState<Pair<Boolean, ProfileEntity?>>((id == null) to null, id) {
                value = if (id == null) true to null else true to vm.get(id)
            }
            if (loaded.first) {
                val existing = loaded.second
                EditScreen(
                    existing = existing,
                    onSave = { name, url, note, manualHue, icon, count ->
                        if (existing == null) vm.createBatch(name, url, note, manualHue, icon, count)
                        else {
                            val color = manualHue?.let { Color.hsv(it, 0.65f, 0.85f).toArgb() } ?: existing.colorArgb
                            val upd = existing.copy(name = name, url = url, note = note, colorArgb = color, iconKey = icon)
                            vm.update(upd); tabs.updateProfile(upd)
                        }
                        screen = Screen.LIST
                    },
                    onBack = { screen = Screen.LIST },
                )
            }
        }
        Screen.SETTINGS -> SettingsScreen(vm, onBack = { screen = Screen.LIST })
        Screen.BROWSER -> BrowserScreen(
            tabs,
            onShowProfiles = { screen = Screen.LIST },
            onShortcut = { p ->
                val ok = ShortcutHelper.requestPin(ctx, p)
                Toast.makeText(ctx, if (ok) R.string.shortcut_requested else R.string.shortcut_unsupported, Toast.LENGTH_LONG).show()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileListScreen(
    vm: ProfileViewModel,
    tabs: TabManager,
    onOpen: (ProfileEntity) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ProfileEntity) -> Unit,
    onSettings: () -> Unit,
    onBrowser: () -> Unit,
    onDelete: (ProfileEntity) -> Unit,
) {
    val ctx = LocalContext.current
    val profiles by vm.profiles.collectAsState()
    val query by vm.query.collectAsState()
    var toDelete by remember { mutableStateOf<ProfileEntity?>(null) }
    val supported = ProfileEngine.isSupported()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (supported) ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add_profile)) },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        ctx.resources.getQuantityString(R.plurals.profile_count, profiles.size, profiles.size),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (tabs.tabs.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(34.dp).clickable(onClick = onBrowser),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${tabs.tabs.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
            }
            if (!supported) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text(stringResource(R.string.unsupported), Modifier.padding(start = 10.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            PillField(
                value = query, onValueChange = { vm.query.value = it },
                placeholder = stringResource(R.string.search_hint),
                leading = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )
            if (profiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_state), Modifier.padding(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(profiles, key = { it.id }) { p ->
                        ProfileRow(
                            p, isOpen = tabs.isOpen(p.id),
                            onOpen = { onOpen(p) },
                            onFavorite = { vm.toggleFavorite(p) },
                            onEdit = { onEdit(p) },
                            onShortcut = {
                                val ok = ShortcutHelper.requestPin(ctx, p)
                                Toast.makeText(ctx, if (ok) R.string.shortcut_requested else R.string.shortcut_unsupported, Toast.LENGTH_LONG).show()
                            },
                            onDelete = { toDelete = p },
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_msg, p.name)) },
            confirmButton = { TextButton(onClick = { onDelete(p); toDelete = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ProfileRow(
    p: ProfileEntity,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onShortcut: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(p.name, p.iconKey, Color(p.colorArgb), 46.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (isOpen) Box(Modifier.padding(start = 8.dp).size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                }
                Text(if (p.note.isBlank()) hostOf(p.url) else p.note + " · " + hostOf(p.url), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (p.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, stringResource(R.string.favorite),
                    tint = if (p.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.shortcut)) }, onClick = { menu = false; onShortcut() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreen(
    existing: ProfileEntity?,
    onSave: (name: String, url: String, note: String, manualHue: Float?, icon: String, count: Int) -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var url by remember { mutableStateOf(existing?.url ?: homepagePref(ctx)) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var icon by remember { mutableStateOf(existing?.iconKey ?: ProfileIcons.DEFAULT) }
    var countText by remember { mutableStateOf("1") }
    var manual by remember { mutableStateOf(false) }
    var hue by remember {
        mutableFloatStateOf(
            existing?.let { val h = FloatArray(3); android.graphics.Color.colorToHSV(it.colorArgb, h); h[0] } ?: 230f
        )
    }
    var nameError by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf(false) }
    var countError by remember { mutableStateOf(false) }
    val previewColor = when {
        manual -> Color.hsv(hue, 0.65f, 0.85f)
        existing != null -> Color(existing.colorArgb)
        else -> Color(AutoColor.forIndex(0))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (existing == null) R.string.add_profile else R.string.edit_profile)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ProfileAvatar(name.ifBlank { "?" }, icon, previewColor, 72.dp)
            }
            OutlinedTextField(
                value = name, onValueChange = { name = it; nameError = false }, singleLine = true,
                label = { Text(stringResource(R.string.profile_name)) }, isError = nameError,
                supportingText = { if (nameError) Text(stringResource(R.string.name_required)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note, onValueChange = { note = it }, singleLine = true,
                label = { Text(stringResource(R.string.note_field)) },
                supportingText = { Text(stringResource(R.string.note_help)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = url, onValueChange = { url = it; urlError = false }, singleLine = true,
                label = { Text(stringResource(R.string.website_url)) }, isError = urlError,
                supportingText = { if (urlError) Text(stringResource(R.string.invalid_url)) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (existing == null) {
                OutlinedTextField(
                    value = countText, onValueChange = { countText = it.filter(Char::isDigit).take(4); countError = false },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(R.string.count_field)) }, isError = countError,
                    supportingText = { Text(stringResource(if (countError) R.string.count_invalid else R.string.count_help)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(stringResource(R.string.icon), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = icon == ProfileIcons.LETTER, onClick = { icon = ProfileIcons.LETTER }, label = { Text("Aa") })
                }
                items(ProfileIcons.all.keys.toList()) { key ->
                    FilterChip(selected = icon == key, onClick = { icon = key }, label = { Icon(ProfileIcons.get(key), null, Modifier.size(20.dp)) })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.color_manual), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.color_auto_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = manual, onCheckedChange = { manual = it })
            }
            if (manual) Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
            Text(stringResource(R.string.embedded_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = {
                    val n = name.trim()
                    val u = UrlUtils.normalize(url)
                    val c = if (existing == null) countText.toIntOrNull() else 1
                    nameError = n.isEmpty()
                    urlError = u == null
                    countError = c == null || c < 1 || c > 9999
                    if (!nameError && u != null && !countError && c != null) {
                        onSave(n, u, note.trim(), if (manual) hue else null, icon, c)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(vm: ProfileViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var home by remember { mutableStateOf(homepagePref(ctx)) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = home,
                onValueChange = { home = it; UrlUtils.normalize(it)?.let { n -> setHomepagePref(ctx, n) } },
                singleLine = true, label = { Text(stringResource(R.string.default_homepage)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Section(R.string.privacy, R.string.privacy_text)
            Section(R.string.downloads, R.string.downloads_text)
            Section(R.string.permissions, R.string.permissions_text)
            Text(stringResource(R.string.embedded_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.dev_tools), style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = { vm.createTestPair() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_test_pair)) }
            OutlinedButton(onClick = { vm.createStressProfiles(100) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_100)) }
        }
    }
}

@Composable
private fun Section(title: Int, body: Int) {
    Column {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
