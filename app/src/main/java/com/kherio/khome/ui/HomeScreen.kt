@file:OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)

package com.kherio.khome.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.isActive
import kotlin.math.abs
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.kherio.khome.HomeViewModel
import com.kherio.khome.R
import com.kherio.khome.data.AppEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
private val NoAutoScroll = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

/**
 * Desplazamiento automático al enfocar: mueve lo mínimo para que el elemento quede dentro con un
 * margen, y lo hace con una animación larga y suave en lugar del salto rápido por defecto.
 */
@OptIn(ExperimentalFoundationApi::class)
private val SmoothSpec = object : BringIntoViewSpec {
    override val scrollAnimationSpec: AnimationSpec<Float> =
        spring(dampingRatio = 1f, stiffness = 120f)

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val margin = containerSize * 0.06f
        val leading = offset - margin
        val trailing = offset + size + margin
        return when {
            leading >= 0f && trailing <= containerSize -> 0f
            size + 2 * margin >= containerSize -> offset - margin
            leading < 0f -> leading
            else -> trailing - containerSize
        }
    }
}

private val SidePadding = 56.dp
private val CardGap = 16.dp

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val k = LocalK.current

    val apps by vm.visibleApps.collectAsState()
    val settings by vm.settings.collectAsState()
    val weather by vm.weather.collectAsState()
    val update by vm.update.collectAsState()
    val recent by vm.recentApps.collectAsState()
    val hiddenApps by vm.hiddenApps.collectAsState()
    var showHiddenList by remember { mutableStateOf(false) }
    val net = rememberNetState()

    var showSettings by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var movingPkg by remember { mutableStateOf<String?>(null) }
    var swallowOkUp by remember { mutableStateOf(false) }
    var inputs by remember { mutableStateOf<List<TvInput>?>(null) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var lastFocusedPkg by remember { mutableStateOf<String?>(null) }
    var lastFocusedRecent by remember { mutableStateOf(false) }
    var menuFromRecent by remember { mutableStateOf(false) }

    val requesters = remember { HashMap<String, FocusRequester>() }
    fun requesterFor(pkg: String) = requesters.getOrPut(pkg) { FocusRequester() }

    val recentRequesters = remember { HashMap<String, FocusRequester>() }
    fun recentRequesterFor(pkg: String) = recentRequesters.getOrPut(pkg) { FocusRequester() }

    val settingsFocus = remember { FocusRequester() }
    val inputsFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val pageState = rememberLazyListState()
    val newsRows by vm.newsRows.collectAsState()
    var newsFocused by remember { mutableStateOf(false) }
    // Fila con el foco: 0 apps, 1 recientes, 2 novedades. Las demás se atenúan un poco.
    var activeRow by remember { mutableIntStateOf(0) }
    val appsAlpha by animateFloatAsState(if (activeRow == 0) 1f else 0.72f, tween(350), label = "a0")
    val recentAlpha by animateFloatAsState(if (activeRow == 1) 1f else 0.72f, tween(350), label = "a1")
    val newsAlpha by animateFloatAsState(if (activeRow == 2) 1f else 0.72f, tween(350), label = "a2")
    var wasMoving by remember { mutableStateOf(false) }
    val cardGapPx = with(androidx.compose.ui.platform.LocalDensity.current) { CardGap.roundToPx() }

    val perPage = settings.perPage.coerceIn(3, 6)
    val pageCount = if (apps.isEmpty()) 0 else (apps.size + perPage - 1) / perPage
    val page = if (apps.isEmpty()) 0 else (focusedIndex.coerceIn(0, apps.lastIndex)) / perPage

    // Foco inicial en la primera tarjeta.
    var initialFocusDone by remember { mutableStateOf(false) }
    LaunchedEffect(apps.isNotEmpty()) {
        if (apps.isNotEmpty() && lastFocusedPkg == null && !initialFocusDone) {
            initialFocusDone = true
            delay(150)
            runCatching { requesterFor(apps.first().packageName).requestFocus() }
        }
    }

    // Desplazamiento propio y suave: una tarjeta cada vez, solo cuando el foco
    // llega al borde. El desplazamiento automático de Compose está desactivado.
    LaunchedEffect(focusedIndex, perPage) {
        if (movingPkg != null) return@LaunchedEffect
        val first = listState.firstVisibleItemIndex
        val itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return@LaunchedEffect
        val step = (itemSize + cardGapPx).toFloat()
        val delta = when {
            focusedIndex >= first + perPage -> focusedIndex - (first + perPage - 1)
            focusedIndex < first -> focusedIndex - first
            else -> 0
        }
        if (delta != 0) {
            listState.animateScrollBy(step * delta, tween(320, easing = FastOutSlowInEasing))
        }
    }

    // Al mover una app, la cámara la sigue: se queda en el centro y las vecinas se apartan.
    LaunchedEffect(movingPkg) {
        val pkg = movingPkg
        if (pkg != null) {
            wasMoving = true
            while (isActive) {
                withFrameNanos { }
                val info = listState.layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { it.key == pkg } ?: continue
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val delta = item.offset + item.size / 2f - center
                if (abs(delta) > 0.5f) listState.scrollBy(delta * 0.18f)
            }
        } else if (wasMoving) {
            wasMoving = false
            val target = (focusedIndex - perPage / 2).coerceIn(0, maxOf(0, apps.size - perPage))
            listState.animateScrollToItem(target)
        }
    }

    // Devuelve el foco a la última tarjeta cuando se cierra un menú.
    LaunchedEffect(menuApp == null, showHiddenList) {
        if (menuApp == null && !showHiddenList) {
            val pkg = lastFocusedPkg
            if (pkg != null) {
                delay(120)
                runCatching {
                    if (lastFocusedRecent) recentRequesterFor(pkg).requestFocus()
                    else requesterFor(pkg).requestFocus()
                }
            }
        }
    }
    LaunchedEffect(inputs == null) {
        if (inputs == null && lastFocusedPkg != null) {
            delay(120)
            runCatching { inputsFocus.requestFocus() }
        }
    }

    fun goHome() {
        showSettings = false
        menuApp = null
        movingPkg = null
        inputs = null
        focusedIndex = 0
        newsFocused = false
        scope.launch {
            pageState.animateScrollToItem(0)
            listState.animateScrollToItem(0)
            apps.firstOrNull()?.let { runCatching { requesterFor(it.packageName).requestFocus() } }
        }
    }

    LaunchedEffect(Unit) { vm.homePressed.collect { goHome() } }

    // Atrás nunca saca al usuario del launcher.
    BackHandler {
        when {
            showSettings -> {
                showSettings = false
                scope.launch {
                    delay(250)
                    runCatching { settingsFocus.requestFocus() }
                }
            }
            movingPkg != null -> movingPkg = null
            else -> goHome()
        }
    }

    KBackground {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp)) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides SmoothSpec) {
            LazyColumn(
                state = pageState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
            // Parte alta: tiempo a la izquierda, reloj a la derecha. Es la primera fila de la
            // lista, así que sube y baja con el mismo desplazamiento suave que las demás.
            item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SidePadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                if (settings.weatherEnabled) {
                    WeatherWidget(weather = weather, cityName = settings.cityName)
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                ClockWidget(use24h = settings.use24h)
            }
            }
            item {
            Column(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = appsAlpha }) {
            if (movingPkg != null) {
                Text(
                    text = stringResource(R.string.move_hint),
                    color = k.accent,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 4.dp),
                )
            }

            // Carrusel de apps.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth = (maxWidth - SidePadding * 2 - CardGap * (perPage - 1)) / perPage
                val cardHeight = cardWidth / 1.58f
                // Al mover, el relleno lateral crece para que cualquier tarjeta pueda quedar centrada.
                val rowPad by animateDpAsState(
                    if (movingPkg != null) (maxWidth - cardWidth) / 2 else SidePadding,
                    tween(380, easing = FastOutSlowInEasing),
                    label = "pad",
                )
                CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoScroll) {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight + 56.dp),
                    contentPadding = PaddingValues(horizontal = rowPad, vertical = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(CardGap),
                ) {
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        val moving = movingPkg == app.packageName
                        AppCard(
                            app = app,
                            moving = moving,
                            useBanner = settings.cardStyle == 1,
                            focusRequester = requesterFor(app.packageName),
                            onFocusChange = { has ->
                                if (has) {
                                    focusedIndex = index
                                    lastFocusedPkg = app.packageName
                                    lastFocusedRecent = false
                                    newsFocused = false
                                    activeRow = 0
                                }
                            },
                            onClick = {
                                vm.recordLaunch(app.packageName)
                                context.launchApp(app)
                            },
                            onLongClick = {
                                menuFromRecent = false
                                menuApp = app
                            },
                            modifier = Modifier
                                .animateItem()
                                .width(cardWidth)
                                .onPreviewKeyEvent { e ->
                                    val isOk = e.key == Key.DirectionCenter || e.key == Key.Enter ||
                                        e.key == Key.NumPadEnter
                                    if (!moving) {
                                        // Al terminar de mover, el OK que suelta la tecla no debe abrir la app.
                                        if (swallowOkUp && isOk && e.type == KeyEventType.KeyUp) {
                                            swallowOkUp = false
                                            return@onPreviewKeyEvent true
                                        }
                                        return@onPreviewKeyEvent false
                                    }
                                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
                                    when (e.key) {
                                        Key.DirectionLeft -> {
                                            vm.moveApp(app.packageName, -1)
                                            focusedIndex = (index - 1).coerceAtLeast(0)
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            vm.moveApp(app.packageName, 1)
                                            focusedIndex = (index + 1).coerceAtMost(apps.lastIndex)
                                            true
                                        }
                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                            movingPkg = null
                                            swallowOkUp = true
                                            true
                                        }
                                        else -> true // Arriba/abajo no hacen nada mientras se mueve.
                                    }
                                },
                        )
                    }
                }
                }
            }

            PageDots(
                count = pageCount,
                current = page,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            }
            }

            if (settings.showRecent && recent.isNotEmpty()) item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = recentAlpha }) {
                    val rowCardWidth =
                        (maxWidth - SidePadding * 2 - CardGap * (perPage - 1)) / perPage * 0.72f
                    Column {
                        Text(
                            text = stringResource(R.string.recent),
                            color = k.textSecondary,
                            fontSize = 15.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            modifier = Modifier.padding(start = SidePadding + 4.dp, top = 6.dp),
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = SidePadding, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(CardGap - 4.dp),
                        ) {
                            items(recent, key = { it.packageName }) { app ->
                                AppCard(
                                    app = app,
                                    compact = true,
                                    useBanner = settings.cardStyle == 1,
                                    focusRequester = recentRequesterFor(app.packageName),
                                    onFocusChange = { has ->
                                        if (has) {
                                            lastFocusedPkg = app.packageName
                                            lastFocusedRecent = true
                                            newsFocused = false
                                            activeRow = 1
                                        }
                                    },
                                    onClick = {
                                        vm.recordLaunch(app.packageName)
                                        context.launchApp(app)
                                    },
                                    onLongClick = {
                                        menuFromRecent = true
                                        menuApp = app
                                    },
                                    modifier = Modifier.width(rowCardWidth),
                                )
                            }
                        }
                    }
                }
            }

            items(newsRows, key = { it.platform.id }) { row ->
                Column(modifier = Modifier.graphicsLayer { alpha = newsAlpha }) {
                    Text(
                        text = stringResource(R.string.news_title, row.platform.name),
                        color = k.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        modifier = Modifier.padding(start = SidePadding + 4.dp, top = 6.dp),
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = SidePadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(row.titles, key = { "${it.id}-${it.name}" }) { title ->
                            PosterCard(
                                title = title,
                                modifier = Modifier.width(104.dp),
                                onFocusChange = { has ->
                                    if (has) {
                                        newsFocused = true
                                        activeRow = 2
                                    }
                                },
                                onClick = { context.launchApp(row.app) },
                            )
                        }
                    }
                }
            }
            }
            }

            BottomBar(
                raised = settings.raisedButtons,
                net = net,
                update = update,
                onInputs = {
                    val list = context.passthroughInputs()
                    if (list.isEmpty()) {
                        Toast.makeText(context, R.string.toast_no_inputs, Toast.LENGTH_SHORT).show()
                    } else {
                        inputs = list
                    }
                },
                onNetwork = { context.openWifiSettings() },
                onVpn = { context.openVpnSettings() },
                onUpdate = { showSettings = true },
                onSettings = { showSettings = true },
                inputsFocus = inputsFocus,
                settingsFocus = settingsFocus,
                modifier = Modifier.padding(horizontal = SidePadding),
            )
        }

        // Panel de ajustes por la derecha.
        AnimatedVisibility(
            visible = showSettings,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            SettingsPanel(vm)
        }
    }

    // Menú contextual de una app (pulsación larga).
    menuApp?.let { app ->
        MenuDialog(
            title = app.label,
            entries = listOfNotNull(
                if (menuFromRecent) null else MenuEntry(stringResource(R.string.menu_move)) {
                    movingPkg = app.packageName
                    menuApp = null
                },
                MenuEntry(stringResource(R.string.menu_hide)) {
                    vm.hide(app.packageName)
                    lastFocusedPkg = null
                    menuApp = null
                    goHome()
                },
                if (hiddenApps.isEmpty()) null else MenuEntry(
                    stringResource(R.string.menu_show_hidden, hiddenApps.size),
                ) {
                    menuApp = null
                    showHiddenList = true
                },
                MenuEntry(stringResource(R.string.menu_close)) {
                    context.closeApp(app)
                    menuApp = null
                },
                MenuEntry(stringResource(R.string.menu_info)) {
                    context.openAppInfo(app)
                    menuApp = null
                },
                MenuEntry(stringResource(R.string.menu_uninstall)) {
                    context.uninstallApp(app)
                    menuApp = null
                },
            ),
            onDismiss = { menuApp = null },
        )
    }

    // Lista de apps ocultas para volver a mostrarlas.
    if (showHiddenList) {
        MenuDialog(
            title = stringResource(R.string.hidden_title),
            entries = hiddenApps.map { hidden ->
                MenuEntry(hidden.label) {
                    vm.unhide(hidden.packageName)
                    showHiddenList = false
                }
            },
            onDismiss = { showHiddenList = false },
        )
    }

    // Selector de entradas HDMI.
    inputs?.let { list ->
        MenuDialog(
            title = stringResource(R.string.cd_inputs),
            entries = list.map { input ->
                MenuEntry(input.label) {
                    context.openInput(input)
                    inputs = null
                }
            },
            onDismiss = { inputs = null },
        )
    }
}
