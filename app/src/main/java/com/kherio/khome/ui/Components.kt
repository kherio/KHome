@file:OptIn(ExperimentalFoundationApi::class, ExperimentalTvMaterial3Api::class)

package com.kherio.khome.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.text.format.DateFormat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.kherio.khome.R
import com.kherio.khome.data.AppEntry
import com.kherio.khome.update.UpdateState
import com.kherio.khome.weather.WeatherData
import com.kherio.khome.weather.weatherDescription
import com.kherio.khome.weather.weatherKind
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Pide el foco a este [FocusRequester] cuando el composable aparece en pantalla. */
@Composable
fun RequestFocusOnShow(requester: FocusRequester, key: Any? = Unit) {
    LaunchedEffect(key) {
        delay(60)
        runCatching { requester.requestFocus() }
    }
}

/**
 * Base de todos los elementos enfocables con el mando: escala, borde dorado y brillo al enfocar.
 * Usa solo foundation para no depender de detalles de la API de tv-material.
 */
@Composable
fun Focusable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    background: Color = Color.Transparent,
    backgroundFocused: Color = background,
    idleBorder: Color = Color.Transparent,
    focusScale: Float = 1.06f,
    glow: Boolean = true,
    glowRadius: Dp = 18.dp,
    lift: Boolean = false,
    forceHighlight: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val k = LocalK.current
    var focused by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }
    val highlighted = focused || forceHighlight
    val scale by animateFloatAsState(
        if (highlighted) focusScale else 1f,
        tween(220, easing = FastOutSlowInEasing),
        label = "scale",
    )
    // 0 → 1 al enfocar: anima borde, fondo y halo a la vez, sin saltos.
    val p by animateFloatAsState(
        if (highlighted) 1f else 0f,
        tween(220, easing = FastOutSlowInEasing),
        label = "focus",
    )

    var base = modifier
    if (focusRequester != null) base = base.focusRequester(focusRequester)

    Box(
        modifier = base
            .zIndex(if (highlighted) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange?.invoke(it.isFocused)
            }
            .drawBehind {
                // Sombra suave debajo de los botones con relieve.
                if (lift && p < 1f) {
                    val base = minOf(glowRadius.toPx(), size.minDimension / 2f)
                    for (i in 1..3) {
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.10f * (1f - p)),
                            topLeft = Offset(-i * 1.2f, 2.dp.toPx() - i * 1.2f + i),
                            size = Size(size.width + i * 2.4f, size.height + i * 2.4f),
                            cornerRadius = CornerRadius(base + i * 1.2f),
                        )
                    }
                }
                // Halo dorado por fuera del borde (no se ve a través del fondo).
                if (p > 0.01f && glow) {
                    val step = 4.dp.toPx()
                    val base = minOf(glowRadius.toPx(), size.minDimension / 2f)
                    for (i in 1..4) {
                        val grow = step * i - step / 2f
                        drawRoundRect(
                            color = k.accent.copy(alpha = 0.26f * p / i),
                            topLeft = Offset(-grow, -grow),
                            size = Size(size.width + grow * 2, size.height + grow * 2),
                            cornerRadius = CornerRadius(base + grow),
                            style = Stroke(width = step),
                        )
                    }
                }
            }
            .clip(shape)
            .background(lerp(background, backgroundFocused, p))
            .border(
                width = (1f + 2f * p).dp,
                color = lerp(idleBorder, k.accent, p),
                shape = shape,
            )
            .then(
                if (onLongClick != null) {
                    // Pulsación larga con el mando: OK mantenido genera repeticiones de tecla.
                    Modifier.onPreviewKeyEvent { e ->
                        val isOk = e.key == Key.DirectionCenter || e.key == Key.Enter ||
                            e.key == Key.NumPadEnter
                        if (!isOk) return@onPreviewKeyEvent false
                        when (e.type) {
                            KeyEventType.KeyDown -> {
                                if (e.nativeKeyEvent.repeatCount == 0) {
                                    longFired = false
                                } else if (!longFired) {
                                    longFired = true
                                    onLongClick()
                                }
                                true
                            }
                            KeyEventType.KeyUp -> {
                                if (!longFired) onClick()
                                longFired = false
                                true
                            }
                            else -> false
                        }
                    }
                } else {
                    Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        content = { content(highlighted) },
    )
}

/** Tarjeta de app: solo el icono, centrado; el nombre aparece al enfocar. */
@Composable
fun AppCard(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    moving: Boolean = false,
    compact: Boolean = false,
    useBanner: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    val k = LocalK.current
    Focusable(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.aspectRatio(if (compact) 1.8f else 1.58f),
        shape = RoundedCornerShape(if (compact) 14.dp else 18.dp),
        background = k.card,
        backgroundFocused = k.cardFocused,
        idleBorder = k.cardBorder,
        // Sin escalar: ampliar el bitmap lo difumina. El icono crece por layout (nítido).
        focusScale = 1f,
        glowRadius = if (compact) 14.dp else 18.dp,
        forceHighlight = moving,
        focusRequester = focusRequester,
        onFocusChange = onFocusChange,
    ) { focused ->
        val idle = if (compact) 0.52f else 0.55f
        val big = if (compact) 0.58f else 0.66f
        val fraction by animateFloatAsState(
            if (focused) big else idle,
            tween(220, easing = FastOutSlowInEasing),
            label = "icon",
        )
        val banner = if (useBanner) app.banner else null
        if (banner != null) {
            Image(
                bitmap = banner,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.matchParentSize(),
            )
            return@Focusable
        }

        // Resplandor suave detrás del icono al enfocar.
        if (focused) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(k.accent.copy(alpha = 0.20f), Color.Transparent),
                        ),
                    ),
            )
        }
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight(fraction)
                .aspectRatio(1f)
                .alpha(if (moving) 0.85f else 1f),
        )
    }
}

@Composable
fun RoundIconButton(
    icon: LineIconKind,
    description: String,
    raised: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    tintOverride: Color? = null,
) {
    val k = LocalK.current
    Focusable(
        onClick = onClick,
        modifier = modifier
            .size(50.dp)
            .semantics { contentDescription = description },
        shape = CircleShape,
        background = k.button,
        backgroundFocused = lerp(k.button, k.accent, 0.40f),
        glowRadius = 100.dp,
        lift = raised,
        idleBorder = if (raised) k.buttonBorder else Color.Transparent,
        focusScale = 1.12f,
        focusRequester = focusRequester,
    ) { _ ->
        LineIcon(
            kind = icon,
            tint = tintOverride ?: k.textPrimary,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp),
        )
    }
}

@Composable
fun PillButton(
    text: String,
    raised: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: LineIconKind? = null,
    accent: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    val k = LocalK.current
    val pill = RoundedCornerShape(50)
    Focusable(
        onClick = onClick,
        modifier = modifier
            .height(50.dp),
        shape = pill,
        glowRadius = 100.dp,
        lift = raised && !accent,
        background = if (accent) k.accent.copy(alpha = 0.22f) else k.button,
        backgroundFocused = if (accent) k.accent.copy(alpha = 0.34f) else lerp(k.button, k.accent, 0.40f),
        idleBorder = if (accent) k.accent.copy(alpha = 0.6f) else if (raised) k.buttonBorder else Color.Transparent,
        focusScale = 1.08f,
        focusRequester = focusRequester,
    ) { _ ->
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                LineIcon(icon, if (accent) k.accent else k.textPrimary, Modifier.size(20.dp))
            }
            Text(
                text = text,
                color = if (accent) k.accent else k.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

// --- Reloj y tiempo ---------------------------------------------------------

@Composable
fun ClockWidget(use24h: Boolean, modifier: Modifier = Modifier) {
    val k = LocalK.current
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            val cal = Calendar.getInstance()
            val wait = 60_000L - (cal.get(Calendar.SECOND) * 1000L + cal.get(Calendar.MILLISECOND))
            delay(wait.coerceIn(500L, 60_000L))
        }
    }
    val locale = Locale.getDefault()
    val time = SimpleDateFormat(if (use24h) "HH:mm" else "h:mm", locale).format(now)
    val dateText = SimpleDateFormat(
        DateFormat.getBestDateTimePattern(locale, "EEEEdMMMMy"),
        locale,
    ).format(now).replaceFirstChar { it.titlecase(locale) }

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = time,
            color = k.textPrimary,
            fontSize = 88.sp,
            fontWeight = FontWeight.ExtraLight,
            lineHeight = 92.sp,
        )
        Text(
            text = dateText,
            color = k.textSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

@Composable
fun WeatherWidget(
    weather: WeatherData?,
    cityName: String,
    modifier: Modifier = Modifier,
) {
    val k = LocalK.current

    if (cityName.isBlank()) {
        Text(
            text = stringResource(R.string.weather_hint),
            color = k.textTertiary,
            fontSize = 16.sp,
            modifier = modifier,
        )
        return
    }
    if (weather == null) {
        Text(text = cityName, color = k.textSecondary, fontSize = 20.sp, modifier = modifier)
        return
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(
                kind = weatherKind(weather.code),
                cloudColor = if (k.isDark) Color(0xFFE3E8F0) else Color(0xFF8A94A6),
                backdrop = k.bgTop,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "${weather.temp}°",
                color = k.textPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(weatherDescription(weather.code)),
                color = k.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = cityName + "  ·  " + stringResource(R.string.weather_range, weather.max, weather.min, weather.rainPct),
            color = k.textSecondary,
            fontSize = 15.sp,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.weather_wind, weather.windKmh, weather.humidity),
            color = k.textTertiary,
            fontSize = 15.sp,
            maxLines = 1,
        )
        if (weather.forecast.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                weather.forecast.take(3).forEach { day ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherIcon(
                            kind = weatherKind(day.code),
                            cloudColor = if (k.isDark) Color(0xFFE3E8F0) else Color(0xFF8A94A6),
                            backdrop = k.bgTop,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.forecast_day, day.dayName.take(3), day.max),
                            color = k.textSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// --- Red --------------------------------------------------------------------

data class NetState(val wired: Boolean = false, val wifi: Boolean = false, val vpn: Boolean = false)

@Composable
fun rememberNetState(): NetState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(NetState()) }

    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        @Suppress("DEPRECATION")
        fun compute(): NetState {
            var wired = false
            var wifi = false
            var vpn = false
            for (network in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) vpn = true
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) wired = true
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true
            }
            return NetState(wired, wifi, vpn)
        }

        state = compute()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { state = compute() }
            override fun onLost(network: Network) { state = compute() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { state = compute() }
        }
        runCatching { cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback) }
        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }
    return state
}

// --- Barra inferior ---------------------------------------------------------

@Composable
fun BottomBar(
    raised: Boolean,
    net: NetState,
    update: UpdateState,
    onInputs: () -> Unit,
    onNetwork: () -> Unit,
    onVpn: () -> Unit,
    onUpdate: () -> Unit,
    onSettings: () -> Unit,
    inputsFocus: FocusRequester,
    settingsFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val k = LocalK.current
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                icon = LineIconKind.MONITOR,
                description = stringResource(R.string.cd_inputs),
                raised = raised,
                onClick = onInputs,
                focusRequester = inputsFocus,
            )
            val netIcon = when {
                net.wired -> LineIconKind.ETHERNET
                else -> LineIconKind.WIFI
            }
            RoundIconButton(
                icon = netIcon,
                description = stringResource(
                    when {
                        net.wired -> R.string.net_ethernet
                        net.wifi -> R.string.net_wifi
                        else -> R.string.net_none
                    },
                ),
                raised = raised,
                onClick = onNetwork,
                tintOverride = if (!net.wired && !net.wifi) k.textTertiary else null,
            )
            if (net.vpn) {
                RoundIconButton(
                    icon = LineIconKind.VPN,
                    description = stringResource(R.string.vpn),
                    raised = raised,
                    onClick = onVpn,
                    tintOverride = k.accent,
                )
            }
        }

        when (update) {
            is UpdateState.Available -> PillButton(
                text = stringResource(R.string.update_banner, update.release.version),
                raised = raised,
                onClick = onUpdate,
                accent = true,
                modifier = Modifier.align(Alignment.Center),
            )
            is UpdateState.Downloading -> PillButton(
                text = stringResource(R.string.update_downloading, update.percent),
                raised = raised,
                onClick = onUpdate,
                accent = true,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> Unit
        }

        RoundIconButton(
            icon = LineIconKind.GEAR,
            description = stringResource(R.string.cd_settings),
            raised = raised,
            onClick = onSettings,
            focusRequester = settingsFocus,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    if (count <= 1) return
    val k = LocalK.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val selected = i == current
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (selected) 24.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (selected) k.textPrimary else k.textTertiary),
            )
        }
    }
}

// --- Menús ------------------------------------------------------------------

data class MenuEntry(val label: String, val onClick: () -> Unit)

/** Diálogo con una lista de acciones; el foco empieza en la primera. */
@Composable
fun MenuDialog(title: String, entries: List<MenuEntry>, onDismiss: () -> Unit) {
    val k = LocalK.current
    val first = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        RequestFocusOnShow(first)
        // El menú se abre mientras OK sigue pulsado: hasta que se suelte, ese OK no debe activar nada.
        var armed by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .onPreviewKeyEvent { e ->
                    val isOk = e.key == Key.DirectionCenter || e.key == Key.Enter ||
                        e.key == Key.NumPadEnter
                    if (!isOk || armed) return@onPreviewKeyEvent false
                    when (e.type) {
                        KeyEventType.KeyDown ->
                            if (e.nativeKeyEvent.repeatCount > 0) {
                                true // repetición de la pulsación larga que abrió el menú
                            } else {
                                armed = true // pulsación nueva: funciona con normalidad
                                false
                            }
                        KeyEventType.KeyUp -> {
                            armed = true // se suelta el OK de la pulsación larga
                            true
                        }
                        else -> false
                    }
                }
                .width(360.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(k.panel)
                .border(1.dp, k.panelBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = k.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
            )
            entries.forEachIndexed { index, entry ->
                Focusable(
                    onClick = entry.onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    background = k.chip,
                    backgroundFocused = k.chipSelected,
                    focusScale = 1.02f,
                    glow = false,
                    focusRequester = if (index == 0) first else null,
                ) { _ ->
                    Text(
                        text = entry.label,
                        color = k.textPrimary,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

// --- Novedades --------------------------------------------------------------

/** Cartel de una novedad. Al elegirlo se abre la app de la plataforma. */
@Composable
fun PosterCard(
    title: com.kherio.khome.news.NewsTitle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    val k = LocalK.current
    val image = rememberRemoteImage("https://image.tmdb.org/t/p/w342${title.posterPath}")
    Focusable(
        onClick = onClick,
        modifier = modifier.aspectRatio(2f / 3f),
        shape = RoundedCornerShape(12.dp),
        background = k.card,
        backgroundFocused = k.cardFocused,
        idleBorder = k.cardBorder,
        focusScale = 1f,
        glowRadius = 12.dp,
        onFocusChange = onFocusChange,
    ) { focused ->
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = title.name,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.matchParentSize(),
            )
        }
        if (focused) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.5f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f),
                        ),
                    ),
            )
            Text(
                text = title.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
    }
}
