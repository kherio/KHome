package com.kherio.khome.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "khome_prefs")

data class Settings(
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val columns: Int = 6,
    val use24h: Boolean = true,
)

class PrefsRepository(private val context: Context) {

    private object Keys {
        val FAVORITES = stringPreferencesKey("favorites")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val COLUMNS = intPreferencesKey("columns")
        val USE_24H = booleanPreferencesKey("use_24h")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            favorites = p[Keys.FAVORITES]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            hidden = p[Keys.HIDDEN] ?: emptySet(),
            columns = p[Keys.COLUMNS] ?: 6,
            use24h = p[Keys.USE_24H] ?: true,
        )
    }

    suspend fun setFavorites(list: List<String>) {
        context.dataStore.edit { it[Keys.FAVORITES] = list.joinToString(",") }
    }

    suspend fun setHidden(set: Set<String>) {
        context.dataStore.edit { it[Keys.HIDDEN] = set }
    }

    suspend fun setColumns(value: Int) {
        context.dataStore.edit { it[Keys.COLUMNS] = value }
    }

    suspend fun setUse24h(value: Boolean) {
        context.dataStore.edit { it[Keys.USE_24H] = value }
    }
}
