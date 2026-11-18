package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.raniDataStore: DataStore<Preferences> by preferencesDataStore(name = "rani_settings")

enum class AssistantLanguage(val label: String, val speechTag: String, val displaySub: String) {
    AUTO("Auto (Hindi + English)", "hi-IN", "Hinglish / Bilingual"),
    HINDI("हिन्दी (Hindi)", "hi-IN", "Devanagari & Hindi"),
    ENGLISH("English (India)", "en-IN", "Natural English"),
    HINGLISH("Hinglish", "en-IN", "Hindi + English Mix")
}

data class RaniSettings(
    val micEnabled: Boolean = true,
    val automationEnabled: Boolean = true,
    val wakeWordBackgroundEnabled: Boolean = false,
    val continuousConversation: Boolean = false,
    val language: AssistantLanguage = AssistantLanguage.AUTO,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.08f,
    val confirmBeforeMessage: Boolean = true,
    val confirmBeforeCall: Boolean = true,
    val confirmBeforeShare: Boolean = true,
    val confirmBeforeDelete: Boolean = true,
    val privacyFilterNotifications: Boolean = true,
    val safFolderUri: String = ""
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val MIC_ENABLED = booleanPreferencesKey("mic_enabled")
        val AUTOMATION_ENABLED = booleanPreferencesKey("automation_enabled")
        val WAKE_WORD_BG = booleanPreferencesKey("wake_word_bg")
        val CONTINUOUS_MODE = booleanPreferencesKey("continuous_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val CONFIRM_MSG = booleanPreferencesKey("confirm_msg")
        val CONFIRM_CALL = booleanPreferencesKey("confirm_call")
        val CONFIRM_SHARE = booleanPreferencesKey("confirm_share")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val PRIVACY_FILTER = booleanPreferencesKey("privacy_filter")
        val SAF_FOLDER_URI = stringPreferencesKey("saf_folder_uri")
    }

    val settingsFlow: Flow<RaniSettings> = context.raniDataStore.data.map { prefs ->
        val langName = prefs[Keys.LANGUAGE] ?: AssistantLanguage.AUTO.name
        val lang = AssistantLanguage.entries.firstOrNull { it.name == langName } ?: AssistantLanguage.AUTO
        RaniSettings(
            micEnabled = prefs[Keys.MIC_ENABLED] ?: true,
            automationEnabled = prefs[Keys.AUTOMATION_ENABLED] ?: true,
            wakeWordBackgroundEnabled = prefs[Keys.WAKE_WORD_BG] ?: false,
            continuousConversation = prefs[Keys.CONTINUOUS_MODE] ?: false,
            language = lang,
            ttsSpeechRate = prefs[Keys.TTS_RATE] ?: 1.0f,
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.08f,
            confirmBeforeMessage = prefs[Keys.CONFIRM_MSG] ?: true,
            confirmBeforeCall = prefs[Keys.CONFIRM_CALL] ?: true,
            confirmBeforeShare = prefs[Keys.CONFIRM_SHARE] ?: true,
            confirmBeforeDelete = prefs[Keys.CONFIRM_DELETE] ?: true,
            privacyFilterNotifications = prefs[Keys.PRIVACY_FILTER] ?: true,
            safFolderUri = prefs[Keys.SAF_FOLDER_URI] ?: ""
        )
    }

    suspend fun setMicEnabled(enabled: Boolean) {
        context.raniDataStore.edit { it[Keys.MIC_ENABLED] = enabled }
    }

    suspend fun setAutomationEnabled(enabled: Boolean) {
        context.raniDataStore.edit { it[Keys.AUTOMATION_ENABLED] = enabled }
    }

    suspend fun setWakeWordBackgroundEnabled(enabled: Boolean) {
        context.raniDataStore.edit { it[Keys.WAKE_WORD_BG] = enabled }
    }

    suspend fun setContinuousConversation(enabled: Boolean) {
        context.raniDataStore.edit { it[Keys.CONTINUOUS_MODE] = enabled }
    }

    suspend fun setLanguage(language: AssistantLanguage) {
        context.raniDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setTtsParams(rate: Float, pitch: Float) {
        context.raniDataStore.edit {
            it[Keys.TTS_RATE] = rate.coerceIn(0.6f, 1.5f)
            it[Keys.TTS_PITCH] = pitch.coerceIn(0.8f, 1.4f)
        }
    }

    suspend fun setConfirmationRule(
        confirmMessage: Boolean? = null,
        confirmCall: Boolean? = null,
        confirmShare: Boolean? = null,
        confirmDelete: Boolean? = null
    ) {
        context.raniDataStore.edit { prefs ->
            confirmMessage?.let { prefs[Keys.CONFIRM_MSG] = it }
            confirmCall?.let { prefs[Keys.CONFIRM_CALL] = it }
            confirmShare?.let { prefs[Keys.CONFIRM_SHARE] = it }
            confirmDelete?.let { prefs[Keys.CONFIRM_DELETE] = it }
        }
    }

    suspend fun setPrivacyFilterNotifications(enabled: Boolean) {
        context.raniDataStore.edit { it[Keys.PRIVACY_FILTER] = enabled }
    }

    suspend fun setSafFolderUri(uri: String) {
        context.raniDataStore.edit { it[Keys.SAF_FOLDER_URI] = uri }
    }
}
