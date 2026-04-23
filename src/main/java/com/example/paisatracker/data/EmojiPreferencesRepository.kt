package com.example.paisatracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.emojiDataStore: DataStore<Preferences> by preferencesDataStore(name = "emoji_preferences")

class EmojiPreferencesRepository private constructor(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val MOST_USED_EMOJIS = stringPreferencesKey("most_used_emojis")
    }

    val mostUsedEmojis: Flow<List<String>> = dataStore.data
        .map { preferences ->
            val emojiString = preferences[PreferencesKeys.MOST_USED_EMOJIS] ?: ""
            if (emojiString.isBlank()) emptyList()
            else emojiString.split(",")
        }

    suspend fun recordEmojiUsage(emoji: String) {
        dataStore.edit { preferences ->
            val currentList = (preferences[PreferencesKeys.MOST_USED_EMOJIS] ?: "")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableList()

            currentList.remove(emoji)
            currentList.add(0, emoji)

            if (currentList.size > 20) {
                currentList.removeAt(currentList.size - 1)
            }

            preferences[PreferencesKeys.MOST_USED_EMOJIS] = currentList.joinToString(",")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: EmojiPreferencesRepository? = null

        fun getInstance(context: Context): EmojiPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = EmojiPreferencesRepository(context.emojiDataStore)
                INSTANCE = instance
                instance
            }
        }
    }
}
