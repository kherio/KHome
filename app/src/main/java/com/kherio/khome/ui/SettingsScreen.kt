@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.kherio.khome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.kherio.khome.R
import com.kherio.khome.data.AppEntry
import com.kherio.khome.data.Settings

@Composable
fun SettingsScreen(
    settings: Settings,
    hiddenApps: List<AppEntry>,
    onColumns: (Int) -> Unit,
    onUse24h: (Boolean) -> Unit,
    onUnhide: (String) -> Unit,
) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 96.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.settings),
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                color = Glass.TextPrimary,
            )
        }

        item {
            SettingsPanel(stringResource(R.string.columns_per_row)) {
                listOf(5, 6, 7, 8).forEachIndexed { index, n ->
                    GlassButton(
                        text = n.toString(),
                        onClick = { onColumns(n) },
                        selected = settings.columns == n,
                        modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                    )
                }
            }
        }

        item {
            SettingsPanel(stringResource(R.string.clock_24h)) {
                GlassButton(stringResource(R.string.yes), { onUse24h(true) }, selected = settings.use24h)
                GlassButton(stringResource(R.string.no), { onUse24h(false) }, selected = !settings.use24h)
            }
        }

        item {
            SettingsPanel(stringResource(R.string.system_settings)) {
                GlassButton(stringResource(R.string.system_settings), { context.openSystemSettings() })
                GlassButton(stringResource(R.string.choose_launcher), { context.openHomeSettings() })
            }
        }

        item {
            Text(
                text = stringResource(R.string.hidden_apps),
                fontSize = 18.sp,
                color = Glass.TextSecondary,
            )
        }

        if (hiddenApps.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_hidden),
                    fontSize = 14.sp,
                    color = Glass.TextSecondary,
                )
            }
        } else {
            items(hiddenApps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(app.label, fontSize = 16.sp, color = Glass.TextPrimary)
                    GlassButton(stringResource(R.string.show), { onUnhide(app.packageName) })
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = title, fontSize = 16.sp, color = Glass.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
