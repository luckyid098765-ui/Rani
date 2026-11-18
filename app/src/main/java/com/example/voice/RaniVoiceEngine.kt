package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.preferences.AssistantLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

enum class VoiceOrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class RaniVoiceEngine(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onSpeechError: (String) -> Unit,
    private val onTtsDone: () -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRate = 1.0f
    private var pitch = 1.08f
    private var currentLanguage = AssistantLanguage.AUTO

    private val _orbState = MutableStateFlow(VoiceOrbState.IDLE)
    val orbState: StateFlow<VoiceOrbState> = _orbState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _activeVoiceName = MutableStateFlow("Default Female Voice")
    val activeVoiceName: StateFlow<String> = _activeVoiceName.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                configureFemaleVoice(currentLanguage)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _orbState.value = VoiceOrbState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        if (_orbState.value == VoiceOrbState.SPEAKING) {
                            _orbState.value = VoiceOrbState.IDLE
                        }
                        onTtsDone()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (_orbState.value == VoiceOrbState.SPEAKING) {
                            _orbState.value = VoiceOrbState.IDLE
                        }
                    }
                })
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    fun updateSettings(language: AssistantLanguage, rate: Float, newPitch: Float) {
        currentLanguage = language
        speechRate = rate
        pitch = newPitch
        if (ttsReady) {
            configureFemaleVoice(language)
        }
    }

    private fun configureFemaleVoice(language: AssistantLanguage) {
        val engine = tts ?: return
        val targetLocale = when (language) {
            AssistantLanguage.HINDI -> Locale.forLanguageTag("hi-IN")
            AssistantLanguage.ENGLISH, AssistantLanguage.HINGLISH -> Locale.forLanguageTag("en-IN")
            AssistantLanguage.AUTO -> Locale.forLanguageTag("en-IN")
        }
        engine.language = targetLocale
        engine.setSpeechRate(speechRate)
        engine.setPitch(pitch)

        val voices = runCatching { engine.voices }.getOrNull().orEmpty()
        val bestFemale = selectBestFemaleVoice(voices, targetLocale)
        if (bestFemale != null) {
            engine.voice = bestFemale
            _activeVoiceName.value = bestFemale.name
        } else {
            _activeVoiceName.value = "${targetLocale.displayLanguage} (Female Pitch)"
        }
    }

    private fun selectBestFemaleVoice(voices: Set<Voice>, locale: Locale): Voice? {
        val langPrefix = locale.language
        val matching = voices.filter { it.locale.language == langPrefix }
        val femaleHints = listOf("female", "hia", "hib", "ene", "enc", "sfg", "tpc", "rjs", "wavenet-a", "wavenet-d")
        return matching.firstOrNull { voice ->
            val n = voice.name.lowercase()
            femaleHints.any { hint -> n.contains(hint) } && !n.contains("male")
        } ?: matching.firstOrNull { !it.name.lowercase().contains("male") }
    }

    fun isSpeechRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(language: AssistantLanguage) {
        interruptSpeaking()
        if (!isSpeechRecognitionAvailable()) {
            onSpeechError("On-device Speech Recognition service is not available on this device.")
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _orbState.value = VoiceOrbState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        _orbState.value = VoiceOrbState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                        _rmsLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        _rmsLevel.value = 0f
                        _orbState.value = VoiceOrbState.THINKING
                    }

                    override fun onError(error: Int) {
                        _rmsLevel.value = 0f
                        _orbState.value = VoiceOrbState.IDLE
                        val msg = mapSpeechError(error)
                        if (msg != null) onSpeechError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        _rmsLevel.value = 0f
                        _orbState.value = VoiceOrbState.THINKING
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = matches?.firstOrNull()?.trim().orEmpty()
                        if (best.isNotEmpty()) {
                            onFinalResult(best)
                        } else {
                            _orbState.value = VoiceOrbState.IDLE
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (partial.isNotEmpty()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
            }
        }

        val localeTag = when (language) {
            AssistantLanguage.HINDI -> "hi-IN"
            AssistantLanguage.ENGLISH -> "en-IN"
            AssistantLanguage.HINGLISH -> "en-IN"
            AssistantLanguage.AUTO -> "hi-IN"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        _orbState.value = VoiceOrbState.LISTENING
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        runCatching { speechRecognizer?.stopListening() }
        _rmsLevel.value = 0f
        if (_orbState.value == VoiceOrbState.LISTENING) {
            _orbState.value = VoiceOrbState.IDLE
        }
    }

    fun speak(text: String) {
        if (!ttsReady || text.isBlank()) {
            _orbState.value = VoiceOrbState.IDLE
            return
        }
        // Automatically pick Hindi voice if Devanagari characters are detected
        val containsDevanagari = text.any { it in '\u0900'..'\u097F' }
        val target = if (containsDevanagari) {
            Locale.forLanguageTag("hi-IN")
        } else {
            Locale.forLanguageTag("en-IN")
        }
        tts?.language = target
        val voices = runCatching { tts?.voices }.getOrNull().orEmpty()
        selectBestFemaleVoice(voices, target)?.let { tts?.voice = it }

        _orbState.value = VoiceOrbState.SPEAKING
        val id = UUID.randomUUID().toString()
        tts?.speak(text.take(1000), TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun interruptSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        if (_orbState.value == VoiceOrbState.SPEAKING) {
            _orbState.value = VoiceOrbState.IDLE
        }
    }

    fun setThinking() {
        _orbState.value = VoiceOrbState.THINKING
    }

    fun setIdle() {
        if (_orbState.value != VoiceOrbState.SPEAKING) {
            _orbState.value = VoiceOrbState.IDLE
        }
    }

    fun release() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private fun mapSpeechError(error: Int): String? = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Could not hear clearly. Please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Speech service offline/network unavailable. You can also type commands below."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> null
        else -> "Voice recognition paused ($error)."
    }

    companion object {
        private const val TAG = "RaniVoiceEngine"
    }
}
