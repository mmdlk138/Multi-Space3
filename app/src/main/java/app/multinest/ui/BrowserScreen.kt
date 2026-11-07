package app.multinest.ui

import android.app.Activity
import android.widget.Toast
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.multinest.R
import app.multinest.data.ProfileEntity
import app.multinest.data.UrlUtils

@Composable
fun BrowserScreen(
    manager: TabManager,
    onShowProfiles: () -> Unit,
    onShortcut: (ProfileEntity) -> Unit,
) {
    val ctx = LocalContext.current
    val focus = LocalFocusManager.current
    val tab = manager.active
    if (tab == null) {
        LaunchedEffect(Unit) { onShowProfiles() }
        return
    }

    var fullscreen by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val browserView = remember(tab) { manager.browserViewFor(tab, ctx) }
    val session = tab.session

    LaunchedEffect(tab) {
        val i = manager.tabs.indexOf(tab)
        if (i >= 0) listState.animateScrollToItem(i)
    }

    val window = (ctx as Activity).window
    LaunchedEffect(fullscreen) {
        val c = WindowCompat.getInsetsController(window, window.decorView)
        if (fullscreen) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
            Toast.makeText(ctx, R.string.exit_fullscreen_hint, Toast.LENGTH_SHORT).show()
        } else {
            c.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(Unit) {
        onDispose { WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars()) }
    }

    BackHandler(enabled = tab.canBack && session != null) { session?.goBack() }
    BackHandler(enabled = fullscreen) { fullscreen = false }

    Column(Modifier.fillMaxSize()) {
        if (!fullscreen) {
            // address bar — always on top, shows which profile you are in
            PillField(
                value = tab.address,
                onValueChange = { tab.address = it },
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 8.dp),
                leading = {
                    ProfileAvatar(tab.profile.name, tab.profile.iconKey, Color(tab.profile.colorArgb), 24.dp)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    session?.loadUri(UrlUtils.toAddress(tab.address))
                    focus.clearFocus()
                }),
                ltr = true,
            )
            // tab strip: switch between profiles with one tap
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(manager.tabs, key = { it.id }) { t ->
                    TabChip(
                        t, selected = t === tab,
                        onClick = { manager.select(t.id) },
                        onClose = { manager.close(t.id) },
                    )
                }
            }
            if (tab.progress < 100) {
                LinearProgressIndicator(progress = { tab.progress / 100f }, modifier = Modifier.fillMaxWidth().height(2.dp))
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (browserView == null) {
                Text(stringResource(R.string.engine_error), Modifier.padding(24.dp))
            } else {
                AndroidView(
                    factory = {
                        (browserView.parent as? ViewGroup)?.removeView(browserView)
                        browserView
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (!fullscreen) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { session?.goBack() }, enabled = tab.canBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                    IconButton(onClick = { session?.goForward() }, enabled = tab.canForward) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.forward))
                    }
                    IconButton(onClick = { session?.loadUri(tab.address) }) { Icon(Icons.Filled.Refresh, stringResource(R.string.reload)) }
                    IconButton(onClick = { session?.loadUri(tab.profile.url) }) { Icon(Icons.Filled.Home, stringResource(R.string.home)) }
                    IconButton(onClick = onShowProfiles) { Icon(Icons.Filled.Menu, stringResource(R.string.profiles)) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text((if (tab.desktop) "✓ " else "") + stringResource(R.string.desktop_mode)) },
                                onClick = { menu = false; manager.toggleDesktop(tab) },
                            )
                            DropdownMenuItem(text = { Text(stringResource(R.string.fullscreen)) }, onClick = { menu = false; fullscreen = true })
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bookmark)) },
                                onClick = {
                                    menu = false
                                    tab.address.takeIf { it.startsWith("http://") || it.startsWith("https://") }?.let {
                                        manager.setStartUrl(tab, it)
                                        Toast.makeText(ctx, R.string.bookmark_done, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                            DropdownMenuItem(text = { Text(stringResource(R.string.shortcut)) }, onClick = { menu = false; onShortcut(tab.profile) })
                            DropdownMenuItem(text = { Text(stringResource(R.string.close_tab)) }, onClick = { menu = false; manager.close(tab.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(tab: BrowserTab, selected: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) cs.primaryContainer else cs.surfaceVariant,
        modifier = Modifier.height(36.dp).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(start = 6.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(tab.profile.name, tab.profile.iconKey, Color(tab.profile.colorArgb), 24.dp)
            Text(
                tab.profile.name,
                modifier = Modifier.padding(start = 8.dp).widthIn(max = 110.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) cs.onPrimaryContainer else cs.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (selected) {
                Icon(
                    Icons.Filled.Close, stringResource(R.string.close_tab),
                    modifier = Modifier.padding(start = 6.dp).size(16.dp).clickable(onClick = onClose),
                    tint = cs.onPrimaryContainer,
                )
            }
        }
    }
}
