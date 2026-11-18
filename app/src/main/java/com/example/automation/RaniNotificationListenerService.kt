package com.example.automation

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedNotification(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

class RaniNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isConnected.value = true
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.packageName == packageName) return
        val item = extractNotification(sbn) ?: return
        val updated = (listOf(item) + _notifications.value.filterNot { it.key == item.key }).take(40)
        _notifications.value = updated
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        _notifications.value = _notifications.value.filterNot { it.key == sbn.key }
    }

    private fun refreshActiveNotifications() {
        runCatching {
            val active = activeNotifications.orEmpty()
                .filter { it.packageName != packageName }
                .mapNotNull { extractNotification(it) }
                .sortedByDescending { it.timestamp }
                .take(40)
            _notifications.value = active
        }
    }

    private fun extractNotification(sbn: StatusBarNotification): CapturedNotification? {
        val extras = sbn.notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()?.trim().orEmpty()
        if (title.isBlank() && text.isBlank()) return null

        // Privacy shield: skip notifications that look like OTPs, passwords, or PINs
        val combined = "$title $text".lowercase()
        if (privacyFilterEnabled && (
                combined.contains("otp") ||
                    combined.contains("verification code") ||
                    combined.contains("password") ||
                    combined.contains("cvv") ||
                    combined.contains("upi pin")
                )
        ) {
            return null
        }

        val appLabel = friendlyAppName(sbn.packageName)
        return CapturedNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            appLabel = appLabel,
            title = title.ifBlank { appLabel },
            text = text,
            timestamp = sbn.postTime
        )
    }

    private fun friendlyAppName(pkg: String): String = when (pkg) {
        "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
        "com.instagram.android" -> "Instagram"
        "com.facebook.katana" -> "Facebook"
        "org.telegram.messenger" -> "Telegram"
        "com.google.android.youtube" -> "YouTube"
        "com.android.chrome" -> "Chrome"
        else -> pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    companion object {
        @Volatile
        var privacyFilterEnabled: Boolean = true

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
        val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

        fun clearCaptured() {
            _notifications.value = emptyList()
        }
    }
}
