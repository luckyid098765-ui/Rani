package com.example.phone

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.automation.RaniAccessibilityService
import java.net.URLEncoder

data class ContactMatch(
    val name: String,
    val phoneNumber: String
)

data class SafFileEntry(
    val documentId: String,
    val displayName: String,
    val mimeType: String,
    val uri: Uri,
    val sizeBytes: Long
)

data class ActionExecutionResult(
    val success: Boolean,
    val message: String,
    val needsPermission: String? = null
)

class DeviceController(private val context: Context) {

    enum class SupportedApp(
        val displayName: String,
        val packages: List<String>
    ) {
        YOUTUBE("YouTube", listOf("com.google.android.youtube")),
        WHATSAPP("WhatsApp", listOf("com.whatsapp", "com.whatsapp.w4b")),
        INSTAGRAM("Instagram", listOf("com.instagram.android")),
        FACEBOOK("Facebook", listOf("com.facebook.katana", "com.facebook.lite")),
        TELEGRAM("Telegram", listOf("org.telegram.messenger")),
        CHROME("Google Chrome", listOf("com.android.chrome"))
    }

    fun isAppInstalled(app: SupportedApp): Boolean {
        val pm = context.packageManager
        return app.packages.any { pkg ->
            runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        }
    }

    fun openSupportedApp(app: SupportedApp): ActionExecutionResult {
        val pm = context.packageManager
        for (pkg in app.packages) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return ActionExecutionResult(true, "Opened ${app.displayName}.")
            }
        }
        return ActionExecutionResult(
            false,
            "${app.displayName} is not installed on this device."
        )
    }

    fun searchYouTube(query: String): ActionExecutionResult {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isAppInstalled(SupportedApp.YOUTUBE)) {
                setPackage("com.google.android.youtube")
            }
        }
        return runCatching {
            context.startActivity(ytIntent)
            ActionExecutionResult(true, "Searching YouTube for \"$query\".")
        }.getOrElse {
            ActionExecutionResult(false, "Unable to open YouTube search: ${it.localizedMessage}")
        }
    }

    fun searchWebInChrome(query: String): ActionExecutionResult {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (query.startsWith("http://") || query.startsWith("https://")) {
            query
        } else {
            "https://www.google.com/search?q=$encoded"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isAppInstalled(SupportedApp.CHROME)) {
                setPackage("com.android.chrome")
            }
        }
        return runCatching {
            context.startActivity(intent)
            ActionExecutionResult(true, "Searching in Chrome for \"$query\".")
        }.getOrElse {
            ActionExecutionResult(false, "Unable to open browser: ${it.localizedMessage}")
        }
    }

    fun openWhatsAppChat(contactQuery: String, prefillMessage: String = ""): ActionExecutionResult {
        val contacts = if (hasPermission(Manifest.permission.READ_CONTACTS)) {
            searchContacts(contactQuery)
        } else {
            emptyList()
        }
        val match = contacts.firstOrNull()
        if (match != null) {
            val cleanDigits = match.phoneNumber.replace(Regex("[^0-9+]"), "")
            val encodedMsg = URLEncoder.encode(prefillMessage, "UTF-8")
            val uri = if (prefillMessage.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanDigits&text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanDigits")
            }
            val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (isAppInstalled(SupportedApp.WHATSAPP)) {
                    val pkg = SupportedApp.WHATSAPP.packages.firstOrNull { isPackagePresent(it) }
                    if (pkg != null) setPackage(pkg)
                }
            }
            return runCatching {
                context.startActivity(waIntent)
                ActionExecutionResult(true, "Opening WhatsApp chat with ${match.name}.")
            }.getOrElse {
                ActionExecutionResult(false, "Failed to open WhatsApp chat: ${it.localizedMessage}")
            }
        }

        // Fallback: open WhatsApp and use Accessibility search if enabled (e.g. group chat "Family")
        val opened = openSupportedApp(SupportedApp.WHATSAPP)
        if (!opened.success) return opened
        val service = RaniAccessibilityService.instance
        if (service != null) {
            val searched = service.searchAndOpenInApp(contactQuery)
            return if (searched) {
                ActionExecutionResult(true, "Opened WhatsApp and searched for \"$contactQuery\".")
            } else {
                ActionExecutionResult(true, "Opened WhatsApp. Tap search or enable Accessibility search to jump directly to \"$contactQuery\".")
            }
        }
        return ActionExecutionResult(
            true,
            "Opened WhatsApp. Enable Accessibility Service in Access tab to auto-open group chats like \"$contactQuery\"."
        )
    }

    fun shareMessageViaWhatsApp(message: String, phoneOrNull: String? = null): ActionExecutionResult {
        val pkg = SupportedApp.WHATSAPP.packages.firstOrNull { isPackagePresent(it) }
        if (!phoneOrNull.isNullOrBlank()) {
            val digits = phoneOrNull.replace(Regex("[^0-9+]"), "")
            val encoded = URLEncoder.encode(message, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$digits&text=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (pkg != null) setPackage(pkg)
            }
            context.startActivity(intent)
            return ActionExecutionResult(true, "Prepared WhatsApp message to $phoneOrNull. Tap send in WhatsApp.")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pkg != null) setPackage(pkg)
        }
        val chooser = Intent.createChooser(sendIntent, "Send Message").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(if (pkg != null) sendIntent else chooser)
        return ActionExecutionResult(true, "Prepared message in ${if (pkg != null) "WhatsApp" else "Share sheet"}.")
    }

    fun shareMediaUri(uri: Uri, mimeType: String, caption: String = "", preferWhatsApp: Boolean = false): ActionExecutionResult {
        val pkg = if (preferWhatsApp) SupportedApp.WHATSAPP.packages.firstOrNull { isPackagePresent(it) } else null
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (caption.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            if (pkg != null) setPackage(pkg)
        }
        return runCatching {
            if (pkg != null) {
                context.startActivity(intent)
            } else {
                val chooser = Intent.createChooser(intent, "Share via RANI AI").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
            ActionExecutionResult(true, "Prepared media share${if (pkg != null) " in WhatsApp" else ""}. Confirm send inside the target app.")
        }.getOrElse {
            ActionExecutionResult(false, "Unable to share selected file: ${it.localizedMessage}")
        }
    }

    fun searchContacts(query: String): List<ContactMatch> {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) return emptyList()
        val matches = mutableListOf<ContactMatch>()
        val seen = HashSet<String>()
        val selection = if (query.isBlank()) null else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = if (query.isBlank()) null else arrayOf("%$query%")
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                selection,
                args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && matches.size < 15) {
                    val name = cursor.getString(nameIdx)?.trim().orEmpty()
                    val num = cursor.getString(numIdx)?.trim().orEmpty()
                    if (name.isNotBlank() && num.isNotBlank() && seen.add("$name|$num")) {
                        matches.add(ContactMatch(name, num))
                    }
                }
            }
        }
        return matches
    }

    fun placePhoneCall(numberOrName: String): ActionExecutionResult {
        val resolvedNumber = if (numberOrName.any { it.isDigit() } && numberOrName.count { it.isDigit() } >= 5) {
            numberOrName.filter { it.isDigit() || it == '+' }
        } else {
            searchContacts(numberOrName).firstOrNull()?.phoneNumber
        }

        if (resolvedNumber.isNullOrBlank()) {
            if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
                return ActionExecutionResult(
                    false,
                    "Contacts permission is needed to find \"$numberOrName\".",
                    needsPermission = Manifest.permission.READ_CONTACTS
                )
            }
            return ActionExecutionResult(false, "No contact or phone number found for \"$numberOrName\".")
        }

        val uri = Uri.parse("tel:$resolvedNumber")
        return if (hasPermission(Manifest.permission.CALL_PHONE)) {
            val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            ActionExecutionResult(true, "Calling $resolvedNumber now.")
        } else {
            val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            ActionExecutionResult(true, "Opened phone dialer for $resolvedNumber.")
        }
    }

    fun openCallHistory(): ActionExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = CallLog.Calls.CONTENT_TYPE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            ActionExecutionResult(true, "Opened Call History.")
        }.getOrElse {
            val dialFallback = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(dialFallback)
            ActionExecutionResult(true, "Opened Phone app.")
        }
    }

    @SuppressLint("MissingPermission")
    fun answerIncomingCall(): ActionExecutionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return ActionExecutionResult(false, "Answering calls via API requires Android 8.0+.")
        }
        if (!hasPermission(Manifest.permission.ANSWER_PHONE_CALLS)) {
            return ActionExecutionResult(
                false,
                "Answer Phone Calls permission is required.",
                needsPermission = Manifest.permission.ANSWER_PHONE_CALLS
            )
        }
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return ActionExecutionResult(false, "Telecom service unavailable.")
        return runCatching {
            @Suppress("DEPRECATION")
            telecom.acceptRingingCall()
            ActionExecutionResult(true, "Answered incoming call.")
        }.getOrElse {
            ActionExecutionResult(false, "No ringing call or restricted by system: ${it.localizedMessage}")
        }
    }

    @SuppressLint("MissingPermission")
    fun rejectOrEndCall(): ActionExecutionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return ActionExecutionResult(false, "Ending calls programmatically requires Android 9.0+.")
        }
        if (!hasPermission(Manifest.permission.ANSWER_PHONE_CALLS)) {
            return ActionExecutionResult(
                false,
                "Answer Phone Calls permission is required to reject/end calls.",
                needsPermission = Manifest.permission.ANSWER_PHONE_CALLS
            )
        }
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return ActionExecutionResult(false, "Telecom service unavailable.")
        return runCatching {
            @Suppress("DEPRECATION")
            val ended = telecom.endCall()
            if (ended) ActionExecutionResult(true, "Call ended/rejected.")
            else ActionExecutionResult(false, "No active call to end.")
        }.getOrElse {
            ActionExecutionResult(false, "Unable to end call: ${it.localizedMessage}")
        }
    }

    fun adjustVolume(direction: Int, streamType: Int = AudioManager.STREAM_MUSIC): ActionExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult(false, "Audio service unavailable.")
        am.adjustStreamVolume(streamType, direction, AudioManager.FLAG_SHOW_UI)
        val current = am.getStreamVolume(streamType)
        val max = am.getStreamMaxVolume(streamType).coerceAtLeast(1)
        val pct = (current * 100) / max
        val label = when (direction) {
            AudioManager.ADJUST_RAISE -> "Volume increased to $pct%."
            AudioManager.ADJUST_LOWER -> "Volume lowered to $pct%."
            AudioManager.ADJUST_MUTE -> "Audio muted."
            AudioManager.ADJUST_UNMUTE -> "Audio unmuted ($pct%)."
            else -> "Volume adjusted to $pct%."
        }
        return ActionExecutionResult(true, label)
    }

    fun setVolumePercent(percent: Int, streamType: Int = AudioManager.STREAM_MUSIC): ActionExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult(false, "Audio service unavailable.")
        val max = am.getStreamMaxVolume(streamType)
        val target = ((percent.coerceIn(0, 100) / 100f) * max).toInt()
        am.setStreamVolume(streamType, target, AudioManager.FLAG_SHOW_UI)
        return ActionExecutionResult(true, "Volume set to ${percent.coerceIn(0, 100)}%.")
    }

    fun adjustBrightness(increase: Boolean): ActionExecutionResult {
        return if (Settings.System.canWrite(context)) {
            val current = runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            }.getOrDefault(128)
            val delta = if (increase) 48 else -48
            val updated = (current + delta).coerceIn(15, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, updated)
            val pct = (updated * 100) / 255
            ActionExecutionResult(true, "System brightness set to $pct%.")
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(false, "Please grant Modify System Settings permission to control system brightness.")
        }
    }

    fun controlMediaPlayback(keyCode: Int, label: String): ActionExecutionResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult(false, "Audio service unavailable.")
        val now = SystemClock.uptimeMillis()
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        return ActionExecutionResult(true, "Music playback: $label.")
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "RANI AI Alarm"): ActionExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
            putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            val formatted = "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            ActionExecutionResult(true, "Alarm configured for $formatted.")
        }.getOrElse {
            ActionExecutionResult(false, "No Clock app found to set alarm.")
        }
    }

    fun setTimer(seconds: Int, message: String = "RANI Timer"): ActionExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            ActionExecutionResult(true, "Timer set for ${seconds}s.")
        }.getOrElse {
            ActionExecutionResult(false, "No Clock app found to set timer.")
        }
    }

    fun openSystemSettings(section: String = "main"): ActionExecutionResult {
        val action = when (section.lowercase()) {
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "notifications", "notification" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return runCatching {
            context.startActivity(intent)
            ActionExecutionResult(true, "Opened $section settings.")
        }.getOrElse {
            ActionExecutionResult(false, "Unable to open settings: ${it.localizedMessage}")
        }
    }

    fun searchSafFiles(treeUriString: String, query: String): List<SafFileEntry> {
        if (treeUriString.isBlank()) return emptyList()
        val treeUri = Uri.parse(treeUriString)
        val results = mutableListOf<SafFileEntry>()
        runCatching {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: continue
                    val mime = cursor.getString(mimeIdx) ?: "application/octet-stream"
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    if (query.isBlank() || name.contains(query, ignoreCase = true)) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        results.add(SafFileEntry(docId, name, mime, docUri, size))
                    }
                }
            }
        }
        return results
    }

    fun openSafDocument(uri: Uri, mimeType: String): ActionExecutionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            ActionExecutionResult(true, "Opened selected file.")
        }.getOrElse {
            ActionExecutionResult(false, "No app available to open this file type.")
        }
    }

    fun deleteSafDocument(uri: Uri, fileName: String): ActionExecutionResult {
        return runCatching {
            val deleted = DocumentsContract.deleteDocument(context.contentResolver, uri)
            if (deleted) {
                ActionExecutionResult(true, "Deleted \"$fileName\" via Storage Access Framework.")
            } else {
                ActionExecutionResult(false, "Could not delete \"$fileName\". Ensure folder write access is granted.")
            }
        }.getOrElse {
            ActionExecutionResult(false, "File deletion cancelled or restricted: ${it.localizedMessage}")
        }
    }

    fun readClipboardText(): String {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return ""
        val clip: ClipData = cm.primaryClip ?: return ""
        if (clip.itemCount <= 0) return ""
        return clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty()
    }

    fun runAccessibilityAction(action: (RaniAccessibilityService) -> ActionExecutionResult): ActionExecutionResult {
        val service = RaniAccessibilityService.instance
            ?: return ActionExecutionResult(
                false,
                "Accessibility Service is off. Enable RANI AI in Access tab -> Accessibility Service to automate on-screen taps and scrolling."
            )
        return action(service)
    }

    fun globalBack(): ActionExecutionResult = runAccessibilityAction { service ->
        if (service.runGlobalNavigation(AccessibilityService.GLOBAL_ACTION_BACK)) {
            ActionExecutionResult(true, "Navigated Back.")
        } else {
            ActionExecutionResult(false, "Could not perform Back action.")
        }
    }

    fun globalHome(): ActionExecutionResult = runAccessibilityAction { service ->
        if (service.runGlobalNavigation(AccessibilityService.GLOBAL_ACTION_HOME)) {
            ActionExecutionResult(true, "Returned to Home screen.")
        } else {
            ActionExecutionResult(false, "Could not return Home.")
        }
    }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun isPackagePresent(pkg: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
}
