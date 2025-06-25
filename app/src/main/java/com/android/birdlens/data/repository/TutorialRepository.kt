package com.android.birdlens.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TutorialRepository(private val context: Context) {

    private val mapTutorialShownKey = booleanPreferencesKey("map_tutorial_shown")

    val hasSeenMapTutorial: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[mapTutorialShownKey] ?: false
        }

    suspend fun markMapTutorialAsShown() {
        context.dataStore.edit { settings ->
            settings[mapTutorialShownKey] = true
        }
    }
}