@file:OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.kherio.khome.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.kherio.khome.HomeViewModel
import com.kherio.khome.R
import com.kherio.khome.update.UpdateError
import com.kherio.khome.update.UpdateState

private fun cycle(value: Int, delta: Int, size: Int) = (value + delta + size) % size

/** Panel lateral de ajustes (entra por la derecha). */
@Composable
fun SettingsPanel(vm: HomeViewModel, modifier: Modifier = Modifier) {
    val k = LocalK.current
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val update by vm.update.collectAsState()
    val installed by vm.installedApps.collectAsState()
    val cityResults by vm.cityResults.collectAsState()

    var appsPage by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val first = remember { FocusRequester() }

    BackHandler(enabled = appsPage) { appsPage = false }

    val shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    Column(
        modifier = modifier
            .width(460.dp)
            .fillMaxHeight()
            .clip(shape)
            .background(k.panel)
            .border(1.dp, k.panelBorder, shape)
            .focusProperties { exit = { FocusRequester.Cancel } }
            .focusGroup(),
    ) {
        if (appsPage) {
            AppsChooser(
                vm = vm,
                apps = installed,
                hidden = settings.hidden,
                onBack = { appsPage = false },
            )
        } else {
            RequestFocusOnShow(first)
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.settings_title),
                        color = k.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(R.string.settings_subtitle),
                        color = k.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                    )
                }

                // --- Aspecto
                item { SectionTitle(R.string.sec_appearance) }
                item {
                    val names = listOf(
                        stringResource(R.string.theme_system),
                        stringResource(R.string.theme_light),
                        stringResource(R.string.theme_dark),
                    )
                    OptionRow(
                        label = stringResource(R.string.opt_theme),
                        value = names[settings.theme.coerceIn(0, 2)],
                        onStep = { vm.setTheme(cycle(settings.theme, it, 3)) },
                        focusRequester = first,
                    )
                }
                item {
                    val names = listOf(
                        stringResource(R.string.bg_graphite),
                        stringResource(R.string.bg_navy),
                        stringResource(R.string.bg_forest),
                        stringResource(R.string.bg_wine),
                        stringResource(R.string.bg_black),
                        stringResource(R.string.bg_ocean),
                        stringResource(R.string.bg_sunset),
                        stringResource(R.string.bg_purple),
                        stringResource(R.string.bg_coffee),
                        stringResource(R.string.bg_midnight),
                        stringResource(R.string.bg_auto),
                    )
                    OptionRow(
                        label = stringResource(R.string.opt_background),
                        value = names[settings.background.coerceIn(0, BACKGROUND_COUNT - 1)],
                        onStep = { vm.setBackground(cycle(settings.background, it, BACKGROUND_COUNT)) },
                    )
                }
                item {
                    val names = listOf(
                        stringResource(R.string.bgmode_blobs),
                        stringResource(R.string.bgmode_movies),
                        stringResource(R.string.bgmode_flat),
                    )
                    OptionRow(
                        label = stringResource(R.string.opt_bg_mode),
                        value = names[settings.bgMode.coerceIn(0, 2)],
                        onStep = { vm.setBgMode(cycle(settings.bgMode, it, 3)) },
                    )
                }
                item {
                    OptionRow(
                        label = stringResource(R.string.opt_tint),
                        value = stringResource(if (settings.tintByApp) R.string.yes else R.string.no),
                        onStep = { vm.setTintByApp(!settings.tintByApp) },
                    )
                }
                item {
                    val names = listOf(
                        stringResource(R.string.accent_gold),
                        stringResource(R.string.accent_blue),
                        stringResource(R.string.accent_green),
                        stringResource(R.string.accent_pink),
                        stringResource(R.string.accent_orange),
                        stringResource(R.string.accent_white),
                    )
                    OptionRow(
                        label = stringResource(R.string.opt_accent),
                        value = names[settings.accent.coerceIn(0, ACCENT_COUNT - 1)],
                        onStep = { vm.setAccent(cycle(settings.accent, it, ACCENT_COUNT)) },
                    )
                }
                item {
                    OptionRow(
                        label = stringResource(R.string.opt_card_style),
                        value = stringResource(if (settings.cardStyle == 1) R.string.style_banner else R.string.style_icon),
                        onStep = { vm.setCardStyle(if (settings.cardStyle == 1) 0 else 1) },
                    )
                }
                item {
                    OptionRow(
                        label = stringResource(R.string.opt_buttons),
                        value = stringResource(if (settings.raisedButtons) R.string.btn_raised else R.string.btn_flat),
                        onStep = { vm.setRaisedButtons(!settings.raisedButtons) },
                    )
                }
                item {
                    OptionRow(
                        label = stringResource(R.string.opt_clock),
                        value = stringResource(if (settings.use24h) R.string.clock_24 else R.string.clock_12),
                        onStep = { vm.setUse24h(!settings.use24h) },
                    )
                }

                // --- Apps
                item { SectionTitle(R.string.sec_apps) }
                item {
                    val visible = installed.count { it.packageName !in settings.hidden }
                    ActionRow(
                        label = stringResource(R.string.choose_apps),
                        detail = stringResource(R.string.apps_count, visible, installed.size),
                        onClick = { appsPage = true },
                    )
                }
                item {
                    OptionRow(
                        label = stringResource(R.string.opt_per_page),
                        value = settings.perPage.toString(),
                        onStep = { vm.setPerPage((settings.perPage + it).coerceIn(3, 6)) },
                    )
                }

                item {
                    OptionRow(
                        label = stringResource(R.string.opt_recent),
                        value = stringResource(if (settings.showRecent) R.string.yes else R.string.no),
                        onStep = { vm.setShowRecent(!settings.showRecent) },
                    )
                }

                item {
                    OptionRow(
                        label = stringResource(R.string.opt_news),
                        value = stringResource(if (settings.showNews) R.string.yes else R.string.no),
                        onStep = { vm.setShowNews(!settings.showNews) },
                    )
                }
                if (!com.kherio.khome.news.NewsApi.hasKey) {
                    item {
                        Text(
                            stringResource(R.string.news_no_key),
                            color = k.textTertiary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                    }
                }

                // --- Tiempo
                item { SectionTitle(R.string.sec_weather) }
                item {
                    OptionRow(
                        label = stringResource(R.string.opt_weather),
                        value = stringResource(if (settings.weatherEnabled) R.string.yes else R.string.no),
                        onStep = { vm.setWeatherEnabled(!settings.weatherEnabled) },
                    )
                }
                item {
                    Text(
                        text = if (settings.cityName.isBlank()) {
                            stringResource(R.string.city_none)
                        } else {
                            stringResource(R.string.city_current, settings.cityName)
                        },
                        color = k.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(k.chip)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(color = k.textPrimary, fontSize = 17.sp, fontFamily = InterFamily),
                                cursorBrush = SolidColor(k.accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { vm.searchCity(query) }),
                                decorationBox = { inner ->
                                    if (query.isEmpty()) {
                                        Text(
                                            stringResource(R.string.city_hint),
                                            color = k.textTertiary,
                                            fontSize = 17.sp,
                                        )
                                    }
                                    inner()
                                },
                            )
                        }
                        Focusable(
                            onClick = { vm.searchCity(query) },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            background = k.chip,
                            backgroundFocused = k.chipSelected,
                            focusScale = 1.03f,
                            glow = false,
                        ) { _ ->
                            Text(
                                stringResource(R.string.city_search),
                                color = k.textPrimary,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 14.dp),
                            )
                        }
                    }
                }
                items(cityResults, key = { "${it.lat},${it.lon}" }) { city ->
                    ActionRow(label = city.label, onClick = { vm.selectCity(city) })
                }

                // --- Sistema
                item { SectionTitle(R.string.sec_system) }
                item {
                    ActionRow(
                        label = stringResource(R.string.system_settings),
                        onClick = { context.openSystemSettings() },
                    )
                }
                item {
                    ActionRow(
                        label = stringResource(R.string.choose_launcher),
                        onClick = { context.openHomeSettings() },
                    )
                }

                // --- Actualizaciones
                item { SectionTitle(R.string.sec_updates) }
                item {
                    val status = when (val u = update) {
                        UpdateState.Idle -> stringResource(R.string.update_installed, vm.installedVersion)
                        UpdateState.Checking -> stringResource(R.string.update_checking)
                        UpdateState.UpToDate -> stringResource(R.string.update_uptodate, vm.installedVersion)
                        is UpdateState.Available -> stringResource(R.string.update_available, u.release.version)
                        is UpdateState.Downloading -> stringResource(R.string.update_downloading, u.percent)
                        is UpdateState.Failed -> stringResource(
                            when (u.error) {
                                UpdateError.NETWORK -> R.string.update_err_network
                                UpdateError.NO_APK -> R.string.update_err_noapk
                                UpdateError.DOWNLOAD -> R.string.update_err_download
                                UpdateError.INSTALL -> R.string.update_err_install
                                UpdateError.PERMISSION -> R.string.update_err_perm
                            },
                        )
                    }
                    Text(
                        text = status,
                        color = k.textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
                item {
                    ActionRow(
                        label = stringResource(R.string.update_check),
                        onClick = { vm.checkForUpdate(manual = true) },
                    )
                }
                val canInstall = update is UpdateState.Available ||
                    (update as? UpdateState.Failed)?.release != null
                if (canInstall) {
                    item {
                        ActionRow(
                            label = stringResource(R.string.update_install),
                            onClick = { vm.installUpdate() },
                            accent = true,
                        )
                    }
                }
                item {
                    Text(
                        stringResource(R.string.tmdb_credit),
                        color = k.textTertiary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

/** Lista de todas las apps instaladas para elegir cuáles salen en el escritorio. */
@Composable
private fun AppsChooser(
    vm: HomeViewModel,
    apps: List<com.kherio.khome.data.AppEntry>,
    hidden: Set<String>,
    onBack: () -> Unit,
) {
    val k = LocalK.current
    val first = remember { FocusRequester() }
    RequestFocusOnShow(first)

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                stringResource(R.string.choose_apps),
                color = k.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                stringResource(R.string.choose_apps_hint),
                color = k.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
            )
        }
        item {
            ActionRow(
                label = "‹  " + stringResource(R.string.back),
                onClick = onBack,
                focusRequester = first,
            )
        }
        items(apps, key = { it.packageName }) { app ->
            val visible = app.packageName !in hidden
            Focusable(
                onClick = { vm.setAppVisible(app.packageName, !visible) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                background = k.chip,
                backgroundFocused = k.chipSelected,
                focusScale = 1.02f,
                glow = false,
            ) { _ ->
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        text = app.label,
                        color = if (visible) k.textPrimary else k.textTertiary,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(if (visible) R.string.app_visible else R.string.app_hidden),
                        color = if (visible) k.accent else k.textTertiary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(res: Int) {
    val k = LocalK.current
    Text(
        text = stringResource(res),
        color = k.textTertiary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 6.dp, top = 14.dp),
    )
}

/** Fila con valor que cambia con ◀ ▶ (o con OK, que avanza). */
@Composable
private fun OptionRow(
    label: String,
    value: String,
    onStep: (Int) -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val k = LocalK.current
    Focusable(
        onClick = { onStep(1) },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                    onStep(-1); true
                } else if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) {
                    onStep(1); true
                } else {
                    false
                }
            },
        shape = RoundedCornerShape(14.dp),
        background = k.chip,
        backgroundFocused = k.chipSelected,
        focusScale = 1.02f,
        glow = false,
        focusRequester = focusRequester,
    ) { _ ->
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = k.textPrimary, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Text("‹  $value  ›", color = k.accent, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ActionRow(
    label: String,
    onClick: () -> Unit,
    detail: String? = null,
    accent: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val k = LocalK.current
    Focusable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        background = if (accent) k.accent.copy(alpha = 0.2f) else k.chip,
        backgroundFocused = if (accent) k.accent.copy(alpha = 0.32f) else k.chipSelected,
        focusScale = 1.02f,
        glow = false,
        focusRequester = focusRequester,
    ) { _ ->
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = if (accent) k.accent else k.textPrimary,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (detail != null) {
                Text(detail, color = k.textSecondary, fontSize = 14.sp)
            }
        }
    }
}
