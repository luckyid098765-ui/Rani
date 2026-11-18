package com.example.voice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object WakeWordEventBus {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun emitWakeCommand(command: String) {
        _events.tryEmit(command)
    }

    fun setRunning(running: Boolean) {
        _isServiceRunning.value = running
    }
}

class WakeWordForegroundService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var active = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val hasMic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        active = true
        WakeWordEventBus.setRunning(true)
        mainHandler.post { startWakeLoop() }
        return START_STICKY
    }

    private fun startWakeLoop() {
        if (!active || !SpeechRecognizer.isRecognitionAvailable(this)) {
            return
        }
        runCatching { recognizer?.destroy() }
        recognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    scheduleNextListen(delayMs = 900L)
                }

                override fun onResults(results: Bundle?) {
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    handleSpokenPhrase(spoken)
                    scheduleNextListen(delayMs = 700L)
                }
            })
        }

        val listenIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { recognizer?.startListening(listenIntent) }
            .onFailure {
                Log.w(TAG, "Background speech loop error: ${it.message}")
                scheduleNextListen(delayMs = 2000L)
            }
    }

    private fun handleSpokenPhrase(spoken: String) {
        if (spoken.isBlank()) return
        val lower = spoken.lowercase()
        val wakeTriggers = listOf("hey rani", "rani", "हे रानी", "रानी")
        val matched = wakeTriggers.firstOrNull { lower.contains(it) } ?: return
        val trailing = spoken.substringAfter(matched, "").trim()
        val payload = trailing.ifBlank { "wake" }
        WakeWordEventBus.emitWakeCommand(payload)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WAKE_COMMAND, payload)
        }
        runCatching { startActivity(launchIntent) }
    }

    private fun scheduleNextListen(delayMs: Long) {
        if (!active) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({ startWakeLoop() }, delayMs)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RANI Wake-Word Listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent microphone indicator while Hey Rani background listening is active."
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WakeWordForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.wake_word_notification_title))
            .setContentText(getString(R.string.wake_word_notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }

    override fun onDestroy() {
        active = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.destroy() }
        recognizer = null
        WakeWordEventBus.setRunning(false)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WakeWordService"
        private const val CHANNEL_ID = "rani_wake_word_channel"
        private const val NOTIFICATION_ID = 4041
        const val ACTION_STOP = "com.example.voice.STOP_WAKE_WORD"
        const val EXTRA_WAKE_COMMAND = "wake_command"

        fun start(context: Context) {
            val intent = Intent(context, WakeWordForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordForegroundService::class.java))
        }
    }
}
