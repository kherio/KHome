package com.kherio.khome

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kherio.khome.ui.HomeScreen
import com.kherio.khome.ui.KHomeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsState()
            KHomeTheme(settings) {
                HomeScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pulsar Home estando ya en el launcher: volver arriba y cerrar menús.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.homePressed.tryEmit(Unit)
        }
    }
}
