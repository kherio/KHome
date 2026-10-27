@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kherio.khome.ui

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.kherio.khome.R
import com.kherio.khome.data.AppEntry
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Tarjeta de app
// ---------------------------------------------------------------------------

@Composable
private fun glassBorder(shape: Shape) = ClickableSurfaceDefaults.border(
    border = Border(BorderStroke(1.dp, Glass.Stroke), shape = shape),
    focusedBorder = Border(BorderStroke(3.dp, Glass.StrokeFocused), shape = shape),
)

@Composable
fun AppCard(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Glass.Fill,
                focusedContainerColor = Glass.FillFocused,
                pressedContainerColor = Glass.FillFocused,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
            border = glassBorder(shape),
        ) {
            Image(
                bitmap = app.image,
                contentDescription = app.label,
                contentScale = if (app.isBanner) ContentScale.Crop else ContentScale.Fit,
                modifier = if (app.isBanner) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                },
            )
        }
        if (showLabel) {
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = Glass.TextPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Botón de cristal (menús y ajustes)
// ---------------------------------------------------------------------------

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Glass.Accent.copy(alpha = 0.40f) else Glass.Fill,
            focusedContainerColor = if (selected) Glass.Accent.copy(alpha = 0.60f) else Glass.FillFocused,
            pressedContainerColor = Glass.FillFocused,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        border = glassBorder(shape),
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = Glass.TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Menú contextual (pulsación larga)
// ---------------------------------------------------------------------------

@Composable
fun AppContextMenu(
    app: AppEntry,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onHide: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val first = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

        Column(
            modifier = Modifier
                .width(380.dp)
                .clip(shape)
                .background(Color(0xE60E1322))
                .glass(shape, Glass.FillStrong)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = app.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Glass.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            val full = Modifier.fillMaxWidth()
            GlassButton(stringResource(R.string.menu_open), { onDismiss(); onOpen() }, full.focusRequester(first))
            GlassButton(
                text = stringResource(if (isFavorite) R.string.menu_remove_fav else R.string.menu_add_fav),
                onClick = { onDismiss(); onToggleFavorite() },
                modifier = full,
            )
            if (isFavorite) {
                GlassButton(stringResource(R.string.menu_move_left), { onMoveLeft() }, full)
                GlassButton(stringResource(R.string.menu_move_right), { onMoveRight() }, full)
            }
            GlassButton(stringResource(R.string.menu_hide), { onDismiss(); onHide() }, full)
            GlassButton(stringResource(R.string.menu_info), { onDismiss(); onInfo() }, full)
            GlassButton(stringResource(R.string.menu_uninstall), { onDismiss(); onUninstall() }, full)
        }
    }
}

// ---------------------------------------------------------------------------
// Barra superior: reloj, fecha, red y ajustes
// ---------------------------------------------------------------------------

private data class ClockText(val time: String, val date: String)

@Composable
private fun rememberClock(use24h: Boolean): State<ClockText> =
    produceState(ClockText("", ""), use24h) {
        val locale = Locale.getDefault()
        val timeFormat = SimpleDateFormat(if (use24h) "HH:mm" else "h:mm a", locale)
        val datePattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        val dateFormat = SimpleDateFormat(datePattern, locale)
        while (true) {
            val now = Date()
            value = ClockText(
                time = timeFormat.format(now),
                date = dateFormat.format(now).replaceFirstChar { it.titlecase(locale) },
            )
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }

private enum class NetType { WIFI, ETHERNET, NONE }

@Composable
private fun rememberNetType(): State<NetType> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(NetType.NONE) }
    DisposableEffect(context) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                state.value = if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    NetType.ETHERNET
                } else {
                    NetType.WIFI
                }
            }

            override fun onLost(network: Network) {
                state.value = NetType.NONE
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }
    return state
}

@Composable
fun TopBar(
    use24h: Boolean,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock by rememberClock(use24h)
    val net by rememberNetType()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = clock.time,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = Glass.TextPrimary,
            )
            Text(
                text = clock.date,
                fontSize = 16.sp,
                color = Glass.TextSecondary,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .glass(RoundedCornerShape(50), Glass.Fill)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(
                    if (net == NetType.ETHERNET) R.drawable.ic_ethernet else R.drawable.ic_wifi,
                ),
                contentDescription = null,
                tint = if (net == NetType.NONE) Glass.TextSecondary.copy(alpha = 0.4f) else Glass.TextPrimary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    when (net) {
                        NetType.WIFI -> R.string.net_wifi
                        NetType.ETHERNET -> R.string.net_ethernet
                        NetType.NONE -> R.string.net_none
                    },
                ),
                fontSize = 14.sp,
                color = Glass.TextSecondary,
            )
        }

        Spacer(Modifier.width(16.dp))

        Surface(
            onClick = onSettings,
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Glass.Fill,
                focusedContainerColor = Glass.FillFocused,
                pressedContainerColor = Glass.FillFocused,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
            border = glassBorder(CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.settings),
                tint = Glass.TextPrimary,
                modifier = Modifier
                    .padding(12.dp)
                    .size(24.dp),
            )
        }
    }
}
