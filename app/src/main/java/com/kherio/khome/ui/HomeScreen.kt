@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kherio.khome.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.kherio.khome.HomeViewModel
import com.kherio.khome.R
import com.kherio.khome.data.AppEntry
import com.kherio.khome.data.Settings
import com.kherio.khome.update.UpdateState

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val settings: Settings by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.visibleApps.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val hiddenApps by vm.hiddenApps.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val gridState = rememberLazyGridState()
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    // En un launcher, Atrás no debe cerrar la pantalla de inicio: solo cierra los ajustes.
    BackHandler { if (showSettings) showSettings = false }

    LaunchedEffect(Unit) {
        vm.homePressed.collect {
            showSettings = false
            menuApp = null
            runCatching { gridState.animateScrollToItem(0) }
        }
    }

    GlassBackground {
        if (showSettings) {
            SettingsScreen(
                settings = settings,
                hiddenApps = hiddenApps,
                onColumns = vm::setColumns,
                onUse24h = vm::setUse24h,
                onUnhide = vm::unhide,
                update = update,
                installedVersion = vm.installedVersion,
                onCheckUpdate = { vm.checkForUpdate(manual = true) },
                onInstallUpdate = vm::installUpdate,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 56.dp, vertical = 28.dp),
            ) {
                TopBar(
                    use24h = settings.use24h,
                    onSettings = { showSettings = true },
                    updateVersion = (update as? UpdateState.Available)?.release?.version,
                    onUpdate = { showSettings = true },
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.all_apps),
                    fontSize = 14.sp,
                    color = Glass.TextSecondary,
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(settings.columns),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppCard(
                            app = app,
                            onClick = { context.launchApp(app) },
                            onLongClick = { menuApp = app },
                        )
                    }
                }

                if (favorites.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FavoritesDock(
                        favorites = favorites,
                        onClick = { context.launchApp(it) },
                        onLongClick = { menuApp = it },
                    )
                }
            }
        }
    }

    menuApp?.let { app ->
        AppContextMenu(
            app = app,
            isFavorite = favorites.any { it.packageName == app.packageName },
            onDismiss = { menuApp = null },
            onOpen = { context.launchApp(app) },
            onToggleFavorite = { vm.toggleFavorite(app.packageName) },
            onMoveLeft = { vm.moveFavorite(app.packageName, -1) },
            onMoveRight = { vm.moveFavorite(app.packageName, +1) },
            onHide = { vm.hide(app.packageName) },
            onInfo = { context.openAppInfo(app) },
            onUninstall = { context.uninstallApp(app) },
        )
    }
}

/** Dock de favoritas en la parte inferior, sobre un panel de cristal. */
@Composable
private fun FavoritesDock(
    favorites: List<AppEntry>,
    onClick: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(28.dp), Glass.FillStrong),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        ) {
            items(favorites, key = { it.packageName }) { app ->
                AppCard(
                    app = app,
                    onClick = { onClick(app) },
                    onLongClick = { onLongClick(app) },
                    modifier = Modifier.width(132.dp),
                    showLabel = false,
                )
            }
        }
    }
}
