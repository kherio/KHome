@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kherio.khome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.kherio.khome.R
import com.kherio.khome.status.Connection
import com.kherio.khome.status.SpeedState
import com.kherio.khome.status.SpeedTest
import com.kherio.khome.status.readDeviceInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/** Panel con los datos de la tele y un test de velocidad de descarga. */
@Composable
fun StatusDialog(onDismiss: () -> Unit, onNetworkSettings: () -> Unit) {
    val k = LocalK.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val info = remember { readDeviceInfo(context) }
    var speed by remember { mutableStateOf<SpeedState>(SpeedState.Idle) }
    var job by remember { mutableStateOf<Job?>(null) }
    val first = remember { FocusRequester() }

    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        RequestFocusOnShow(first)
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .width(540.dp)
                .clip(shape)
                .background(k.panel)
                .border(1.dp, k.panelBorder, shape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.status_title),
                color = k.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            InfoRow(
                stringResource(R.string.status_connection),
                stringResource(
                    when (info.connection) {
                        Connection.ETHERNET -> R.string.net_ethernet
                        Connection.WIFI -> R.string.net_wifi
                        Connection.NONE -> R.string.net_none
                    },
                ),
            )
            InfoRow(stringResource(R.string.status_ip), info.ip)
            InfoRow(stringResource(R.string.status_storage), info.storage)
            InfoRow(stringResource(R.string.status_memory), info.memory)
            InfoRow(stringResource(R.string.status_android), info.android)
            InfoRow(stringResource(R.string.status_model), info.model)

            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.status_speed),
                color = k.textSecondary,
                fontSize = 14.sp,
            )
            val mbpsText = { v: Double -> String.format(Locale.getDefault(), "%.1f", v) }
            when (val s = speed) {
                SpeedState.Idle -> Text("—", color = k.textTertiary, fontSize = 40.sp, fontWeight = FontWeight.ExtraLight)
                is SpeedState.Running -> Text(
                    stringResource(R.string.speed_value, mbpsText(s.mbps)),
                    color = k.textPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraLight,
                )
                is SpeedState.Done -> {
                    Text(
                        stringResource(R.string.speed_value, mbpsText(s.mbps)),
                        color = k.accent,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light,
                    )
                    s.latencyMs?.let {
                        Text(stringResource(R.string.status_latency, it.toInt()), color = k.textSecondary, fontSize = 14.sp)
                    }
                }
                SpeedState.Failed -> Text(
                    stringResource(R.string.speed_failed),
                    color = k.textSecondary,
                    fontSize = 16.sp,
                )
            }

            Spacer(Modifier.height(8.dp))
            val running = speed is SpeedState.Running
            DialogButton(
                text = stringResource(
                    when {
                        running -> R.string.speed_running
                        speed is SpeedState.Done || speed is SpeedState.Failed -> R.string.speed_again
                        else -> R.string.speed_start
                    },
                ),
                focusRequester = first,
                onClick = {
                    if (!running) {
                        speed = SpeedState.Running(0.0, null)
                        job = scope.launch { SpeedTest.run().collect { speed = it } }
                    }
                },
            )
            DialogButton(stringResource(R.string.status_wifi_settings), onClick = onNetworkSettings)
            DialogButton(stringResource(R.string.status_close), onClick = onDismiss)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val k = LocalK.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = k.textSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = k.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DialogButton(text: String, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    val k = LocalK.current
    Focusable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(14.dp),
        background = k.chip,
        backgroundFocused = k.chipSelected,
        focusScale = 1.02f,
        glow = false,
        focusRequester = focusRequester,
    ) { _ ->
        Text(
            text,
            color = k.textPrimary,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
        )
    }
}
