package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeMode
import com.example.data.model.BengaliFontOption
import com.example.data.model.WritingStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "sobdosuddhi_settings")

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val bengaliFont: BengaliFontOption = BengaliFontOption.NOTO_SERIF,
    val fontScale: Float = 1.0f,
    val defaultWritingStyle: WritingStyle = WritingStyle.CHOLITO,
    val autoSaveEnabled: Boolean = true,
    val offlineModeForced: Boolean = false,
    val preserveLiteraryVoice: Boolean = true,
    val aiChunkSizeChars: Int = 2200,
    val cloudSyncConsentGranted: Boolean = false
)

class SettingsDataStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BENGALI_FONT = stringPreferencesKey("bengali_font")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val WRITING_STYLE = stringPreferencesKey("writing_style")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val OFFLINE_MODE_FORCED = booleanPreferencesKey("offline_mode_forced")
        val PRESERVE_VOICE = booleanPreferencesKey("preserve_literary_voice")
        val AI_CHUNK_SIZE = intPreferencesKey("ai_chunk_size")
        val CLOUD_SYNC_CONSENT = booleanPreferencesKey("cloud_sync_consent")
    }

    val settingsFlow: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            themeMode = AppThemeMode.fromRaw(prefs[Keys.THEME_MODE]),
            bengaliFont = BengaliFontOption.fromRaw(prefs[Keys.BENGALI_FONT]),
            fontScale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.35f),
            defaultWritingStyle = WritingStyle.fromRaw(prefs[Keys.WRITING_STYLE]),
            autoSaveEnabled = prefs[Keys.AUTO_SAVE] ?: true,
            offlineModeForced = prefs[Keys.OFFLINE_MODE_FORCED] ?: false,
            preserveLiteraryVoice = prefs[Keys.PRESERVE_VOICE] ?: true,
            aiChunkSizeChars = (prefs[Keys.AI_CHUNK_SIZE] ?: 2200).coerceIn(1000, 4000),
            cloudSyncConsentGranted = prefs[Keys.CLOUD_SYNC_CONSENT] ?: false
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setBengaliFont(font: BengaliFontOption) {
        dataStore.edit { it[Keys.BENGALI_FONT] = font.name }
    }

    suspend fun setFontScale(scale: Float) {
        dataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.35f) }
    }

    suspend fun setDefaultWritingStyle(style: WritingStyle) {
        dataStore.edit { it[Keys.WRITING_STYLE] = style.name }
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_SAVE] = enabled }
    }

    suspend fun setOfflineModeForced(forced: Boolean) {
        dataStore.edit { it[Keys.OFFLINE_MODE_FORCED] = forced }
    }

    suspend fun setPreserveLiteraryVoice(preserve: Boolean) {
        dataStore.edit { it[Keys.PRESERVE_VOICE] = preserve }
    }

    suspend fun setAiChunkSize(chars: Int) {
        dataStore.edit { it[Keys.AI_CHUNK_SIZE] = chars.coerceIn(1000, 4000) }
    }

    suspend fun setCloudSyncConsent(granted: Boolean) {
        dataStore.edit { it[Keys.CLOUD_SYNC_CONSENT] = granted }
    }
}
