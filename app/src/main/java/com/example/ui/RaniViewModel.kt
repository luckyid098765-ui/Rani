package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.automation.CapturedNotification
import com.example.automation.RaniAccessibilityService
import com.example.automation.RaniNotificationListenerService
import com.example.data.ai.AiImagePreview
import com.example.data.ai.AiRepository
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.ActivityLogEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryPreferenceEntity
import com.example.data.local.RaniDatabase
import com.example.data.preferences.AssistantLanguage
import com.example.data.preferences.RaniSettings
import com.example.data.preferences.SettingsRepository
import com.example.domain.ConfirmationRequest
import com.example.domain.MultilingualCommandParser
import com.example.domain.ParsedCommand
import com.example.phone.ActionExecutionResult
import com.example.phone.ContactMatch
import com.example.phone.DeviceController
import com.example.phone.SafFileEntry
import com.example.voice.RaniVoiceEngine
import com.example.voice.VoiceOrbState
import com.example.voice.WakeWordEventBus
import com.example.voice.WakeWordForegroundService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RaniUiEvent {
    data class RequestPermission(val permission: String, val reason: String) : RaniUiEvent
    data class LaunchPhotoPicker(val recipientHint: String) : RaniUiEvent
    data object LaunchDocumentPickerForShare : RaniUiEvent
    data object LaunchSafFolderPicker : RaniUiEvent
    data class ShowSnackbar(val message: String) : RaniUiEvent
}

class RaniViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RaniDatabase.getInstance(application)
    private val dao = db.raniDao()
    val settingsRepo = SettingsRepository(application)
    val deviceController = DeviceController(application)
    private val aiRepo: AiRepository = PlaceholderAiRepository()

    val settings: StateFlow<RaniSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, RaniSettings())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = dao.observeActivityLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryPreferenceEntity>> = dao.observeMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = dao.observeChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accessibilityConnected: StateFlow<Boolean> = RaniAccessibilityService.isConnected
    val notificationListenerConnected: StateFlow<Boolean> = RaniNotificationListenerService.isConnected
    val capturedNotifications: StateFlow<List<CapturedNotification>> = RaniNotificationListenerService.notifications
    val wakeWordServiceRunning: StateFlow<Boolean> = WakeWordEventBus.isServiceRunning

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = _pendingConfirmation.asStateFlow()

    private val _contactSearchResults = MutableStateFlow<List<ContactMatch>>(emptyList())
    val contactSearchResults: StateFlow<List<ContactMatch>> = _contactSearchResults.asStateFlow()

    private val _safSearchResults = MutableStateFlow<List<SafFileEntry>>(emptyList())
    val safSearchResults: StateFlow<List<SafFileEntry>> = _safSearchResults.asStateFlow()

    private val _latestImagePreview = MutableStateFlow<AiImagePreview?>(null)
    val latestImagePreview: StateFlow<AiImagePreview?> = _latestImagePreview.asStateFlow()

    private val _uiEvents = MutableSharedFlow<RaniUiEvent>(extraBufferCapacity = 8)
    val uiEvents: SharedFlow<RaniUiEvent> = _uiEvents.asSharedFlow()

    private var pendingPhotoRecipientHint: String = ""

    private val voiceEngine: RaniVoiceEngine = RaniVoiceEngine(
        context = application,
        onPartialResult = { partial ->
            _partialTranscript.value = partial
        },
        onFinalResult = { spoken ->
            _partialTranscript.value = ""
            handleUserInput(spoken, fromVoice = true)
        },
        onSpeechError = { err ->
            _partialTranscript.value = ""
            _uiEvents.tryEmit(RaniUiEvent.ShowSnackbar(err))
        },
        onTtsDone = {
            if (settings.value.continuousConversation && settings.value.micEnabled) {
                viewModelScope.launch {
                    if (deviceController.hasPermission(Manifest.permission.RECORD_AUDIO)) {
                        voiceEngine.startListening(settings.value.language)
                    }
                }
            }
        }
    )

    val orbState: StateFlow<VoiceOrbState> = voiceEngine.orbState
    val rmsLevel: StateFlow<Float> = voiceEngine.rmsLevel
    val activeVoiceName: StateFlow<String> = voiceEngine.activeVoiceName

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { s ->
                voiceEngine.updateSettings(s.language, s.ttsSpeechRate, s.ttsPitch)
                RaniNotificationListenerService.privacyFilterEnabled = s.privacyFilterNotifications
            }
        }
        viewModelScope.launch {
            WakeWordEventBus.events.collect { payload ->
                if (payload == "wake") {
                    startVoiceListening()
                } else {
                    handleUserInput(payload, fromVoice = true)
                }
            }
        }
        seedWelcomeIfEmpty()
    }

    private fun seedWelcomeIfEmpty() {
        viewModelScope.launch {
            val existing = dao.observeChatMessages().first()
            if (existing.isEmpty()) {
                dao.insertChatMessage(
                    ChatMessageEntity(
                        role = "rani",
                        text = "Namaste! Main RANI AI hoon — Your Phone, Your Voice, Your AI.\nTry saying or typing in Hindi, English, or Hinglish:\n• \"Open YouTube\" / \"Instagram scroll reels\"\n• \"Call Mom\" / \"Open chat Family\"\n• \"Volume up\" / \"Set alarm 7 am\"\n• \"Find file invoice\" / \"Summarize\"",
                        isPlaceholderAi = false,
                        actionBadge = "READY"
                    )
                )
            }
        }
    }

    fun handleIncomingIntent(intent: Intent?) {
        val wakePayload = intent?.getStringExtra(WakeWordForegroundService.EXTRA_WAKE_COMMAND)
        if (!wakePayload.isNullOrBlank()) {
            intent.removeExtra(WakeWordForegroundService.EXTRA_WAKE_COMMAND)
            if (wakePayload == "wake") {
                startVoiceListening()
            } else {
                handleUserInput(wakePayload, fromVoice = true)
            }
        }
    }

    fun startVoiceListening() {
        val current = settings.value
        if (!current.micEnabled) {
            _uiEvents.tryEmit(RaniUiEvent.ShowSnackbar("Microphone is disabled in Privacy controls (Access tab)."))
            return
        }
        if (!deviceController.hasPermission(Manifest.permission.RECORD_AUDIO)) {
            _uiEvents.tryEmit(
                RaniUiEvent.RequestPermission(
                    Manifest.permission.RECORD_AUDIO,
                    "Microphone access is needed for voice commands."
                )
            )
            return
        }
        if (orbState.value == VoiceOrbState.LISTENING) {
            voiceEngine.stopListening()
        } else {
            voiceEngine.startListening(current.language)
        }
    }

    fun interruptVoice() {
        voiceEngine.interruptSpeaking()
        voiceEngine.stopListening()
    }

    fun handleUserInput(input: String, fromVoice: Boolean = false) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            voiceEngine.interruptSpeaking()
            voiceEngine.setThinking()
            dao.insertChatMessage(ChatMessageEntity(role = "user", text = trimmed))

            val parsed = MultilingualCommandParser.parse(trimmed)
            executeParsedCommand(parsed, trimmed, speakResult = fromVoice || settings.value.continuousConversation)
        }
    }

    private suspend fun executeParsedCommand(
        parsed: ParsedCommand,
        rawText: String,
        speakResult: Boolean
    ) {
        val currentSettings = settings.value
        val automationBlocked = !currentSettings.automationEnabled

        when (parsed) {
            is ParsedCommand.PaymentAttempt -> {
                _pendingConfirmation.value = ConfirmationRequest.PaymentGuard(parsed.rawCommand)
                respondAndLog(
                    category = "Security",
                    command = rawText,
                    reply = "Payment Protection triggered. RANI never executes payments automatically without your manual action.",
                    status = "Guarded",
                    badge = "PAYMENT GUARD",
                    speak = speakResult
                )
            }

            is ParsedCommand.RememberPreference -> {
                dao.upsertMemory(
                    MemoryPreferenceEntity(
                        prefKey = parsed.key,
                        prefValue = parsed.value,
                        category = "User Approved"
                    )
                )
                respondAndLog(
                    category = "Memory",
                    command = rawText,
                    reply = "Saved to your on-device preferences: \"${parsed.value}\". You can manage or erase memories anytime in Activity.",
                    status = "Saved",
                    badge = "MEMORY SAVED",
                    speak = speakResult
                )
            }

            is ParsedCommand.OpenApp -> {
                if (automationBlocked) {
                    notifyAutomationDisabled(rawText)
                    return
                }
                val res = deviceController.openSupportedApp(parsed.app)
                respondActionResult("App Launch", rawText, res, "APP LAUNCH", speakResult)
            }

            is ParsedCommand.SearchYouTube -> {
                if (automationBlocked) {
                    notifyAutomationDisabled(rawText)
                    return
                }
                val res = deviceController.searchYouTube(parsed.query)
                respondActionResult("YouTube", rawText, res, "YOUTUBE SEARCH", speakResult)
            }

            is ParsedCommand.SearchWeb -> {
                val res = deviceController.searchWebInChrome(parsed.query)
                respondActionResult("Browser", rawText, res, "CHROME SEARCH", speakResult)
            }

            is ParsedCommand.OpenWhatsAppChat -> {
                if (automationBlocked) {
                    notifyAutomationDisabled(rawText)
                    return
                }
                val res = deviceController.openWhatsAppChat(parsed.chatName)
                respondActionResult("WhatsApp", rawText, res, "WHATSAPP CHAT", speakResult)
            }

            is ParsedCommand.SendWhatsAppMessage -> {
                if (currentSettings.confirmBeforeMessage) {
                    _pendingConfirmation.value = ConfirmationRequest.SendWhatsAppMessage(
                        recipient = parsed.recipient,
                        messageText = parsed.message
                    )
                    respondAndLog(
                        category = "WhatsApp",
                        command = rawText,
                        reply = "Confirmation requested before preparing WhatsApp message to ${parsed.recipient}.",
                        status = "Awaiting Confirm",
                        badge = "CONFIRM MESSAGE",
                        speak = speakResult
                    )
                } else {
                    val res = deviceController.openWhatsAppChat(parsed.recipient, parsed.message)
                    respondActionResult("WhatsApp", rawText, res, "WHATSAPP", speakResult)
                }
            }

            is ParsedCommand.WhatsAppCall -> {
                if (currentSettings.confirmBeforeCall) {
                    _pendingConfirmation.value = ConfirmationRequest.PlaceCall(
                        targetDisplay = "WhatsApp: ${parsed.contact}",
                        rawTarget = "whatsapp:${parsed.contact}"
                    )
                    respondAndLog(
                        category = "Calling",
                        command = rawText,
                        reply = "Please confirm opening WhatsApp chat to call ${parsed.contact}.",
                        status = "Awaiting Confirm",
                        badge = "CONFIRM CALL",
                        speak = speakResult
                    )
                } else {
                    val res = deviceController.openWhatsAppChat(parsed.contact)
                    respondActionResult("WhatsApp Call", rawText, res, "WHATSAPP CALL", speakResult)
                }
            }

            is ParsedCommand.ShareLatestPhoto -> {
                pendingPhotoRecipientHint = parsed.recipient
                _uiEvents.tryEmit(RaniUiEvent.LaunchPhotoPicker(parsed.recipient))
                respondAndLog(
                    category = "Media Share",
                    command = rawText,
                    reply = "Opening Android Photo Picker so you can pick the photo to share${if (parsed.recipient.isNotBlank()) " with ${parsed.recipient}" else ""}.",
                    status = "Picker Opened",
                    badge = "PHOTO PICKER",
                    speak = speakResult
                )
            }

            is ParsedCommand.CallContact -> {
                if (currentSettings.confirmBeforeCall) {
                    _pendingConfirmation.value = ConfirmationRequest.PlaceCall(
                        targetDisplay = parsed.target,
                        rawTarget = parsed.target
                    )
                    respondAndLog(
                        category = "Phone",
                        command = rawText,
                        reply = "Confirm placing phone call to ${parsed.target}?",
                        status = "Awaiting Confirm",
                        badge = "CONFIRM CALL",
                        speak = speakResult
                    )
                } else {
                    val res = deviceController.placePhoneCall(parsed.target)
                    respondActionResult("Phone", rawText, res, "CALL", speakResult)
                }
            }

            is ParsedCommand.OpenCallHistory -> {
                val res = deviceController.openCallHistory()
                respondActionResult("Phone", rawText, res, "CALL LOG", speakResult)
            }

            is ParsedCommand.AnswerCall -> {
                val res = deviceController.answerIncomingCall()
                respondActionResult("Phone", rawText, res, "ANSWER CALL", speakResult)
            }

            is ParsedCommand.RejectCall -> {
                val res = deviceController.rejectOrEndCall()
                respondActionResult("Phone", rawText, res, "END CALL", speakResult)
            }

            is ParsedCommand.ScrollFeed -> {
                if (automationBlocked) {
                    notifyAutomationDisabled(rawText)
                    return
                }
                val res = deviceController.runAccessibilityAction { svc ->
                    val ok = svc.performScroll(parsed.forward)
                    ActionExecutionResult(ok, if (ok) "Scrolled feed ${if (parsed.forward) "next" else "previous"}." else "No scrollable feed detected.")
                }
                respondActionResult("Accessibility", rawText, res, "SCROLL", speakResult)
            }

            is ParsedCommand.TapAppControl -> {
                if (automationBlocked) {
                    notifyAutomationDisabled(rawText)
                    return
                }
                val res = deviceController.runAccessibilityAction { svc ->
                    val hit = svc.clickMatchingKeywords(parsed.keywords)
                    if (hit != null) {
                        ActionExecutionResult(true, "Triggered \"$hit\" on active screen.")
                    } else {
                        ActionExecutionResult(false, "Could not find visible \"${parsed.controlName}\" button on screen.")
                    }
                }
                respondActionResult("Accessibility", rawText, res, parsed.controlName.uppercase(), speakResult)
            }

            is ParsedCommand.TypeText -> {
                if (automationBlocked) {
                    notifyAutomationDisabled(rawText)
                    return
                }
                val res = deviceController.runAccessibilityAction { svc ->
                    val ok = svc.typeText(parsed.text)
                    ActionExecutionResult(ok, if (ok) "Typed \"${parsed.text}\" into focused field." else "No editable non-password input field found.")
                }
                respondActionResult("Accessibility", rawText, res, "TYPE TEXT", speakResult)
            }

            is ParsedCommand.ReadVisibleScreen -> {
                if (automationBlocked) {
                    notifyAutomationDisabled(rawText)
                    return
                }
                val res = deviceController.runAccessibilityAction { svc ->
                    ActionExecutionResult(true, svc.readSafeVisibleText())
                }
                respondActionResult("Screen Reader", rawText, res, "SCREEN TEXT", speakResult)
            }

            is ParsedCommand.ReadNotifications -> {
                val list = capturedNotifications.value
                val reply = if (!notificationListenerConnected.value) {
                    "Notification Access is off. Enable it in Access tab so RANI can read incoming notifications when asked."
                } else if (list.isEmpty()) {
                    "No recent non-sensitive notifications captured yet."
                } else {
                    list.take(5).joinToString("\n") { "• [${it.appLabel}] ${it.title}: ${it.text}" }
                }
                respondAndLog("Notifications", rawText, reply, "Executed", "NOTIFICATIONS", speak = speakResult)
            }

            is ParsedCommand.GlobalBack -> {
                val res = deviceController.globalBack()
                respondActionResult("Navigation", rawText, res, "BACK", speakResult)
            }

            is ParsedCommand.GlobalHome -> {
                val res = deviceController.globalHome()
                respondActionResult("Navigation", rawText, res, "HOME", speakResult)
            }

            is ParsedCommand.VolumeAdjust -> {
                val res = deviceController.adjustVolume(parsed.direction, AudioManager.STREAM_MUSIC)
                respondActionResult("Audio", rawText, res, "VOLUME", speakResult)
            }

            is ParsedCommand.BrightnessAdjust -> {
                val res = deviceController.adjustBrightness(parsed.increase)
                respondActionResult("Display", rawText, res, "BRIGHTNESS", speakResult)
            }

            is ParsedCommand.MediaControl -> {
                val (code, label) = when (parsed.action) {
                    "next" -> KeyEvent.KEYCODE_MEDIA_NEXT to "Next Track"
                    "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS to "Previous Track"
                    else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "Play / Pause"
                }
                val res = deviceController.controlMediaPlayback(code, label)
                respondActionResult("Music", rawText, res, label.uppercase(), speakResult)
            }

            is ParsedCommand.SetAlarm -> {
                val res = deviceController.setAlarm(parsed.hour, parsed.minute)
                respondActionResult("Clock", rawText, res, "ALARM", speakResult)
            }

            is ParsedCommand.SetTimer -> {
                val res = deviceController.setTimer(parsed.seconds)
                respondActionResult("Clock", rawText, res, "TIMER", speakResult)
            }

            is ParsedCommand.OpenSettings -> {
                val res = deviceController.openSystemSettings(parsed.section)
                respondActionResult("Settings", rawText, res, "SETTINGS", speakResult)
            }

            is ParsedCommand.FindFile -> {
                val treeUri = currentSettings.safFolderUri
                if (treeUri.isBlank()) {
                    _uiEvents.tryEmit(RaniUiEvent.LaunchSafFolderPicker)
                    respondAndLog(
                        category = "Files",
                        command = rawText,
                        reply = "Select a folder once using the Android Folder Picker (SAF) so RANI can search files without broad storage permissions.",
                        status = "Needs Folder",
                        badge = "SAF FOLDER",
                        speak = speakResult
                    )
                } else {
                    val matches = deviceController.searchSafFiles(treeUri, parsed.query)
                    _safSearchResults.value = matches
                    if (matches.isNotEmpty()) {
                        val first = matches.first()
                        deviceController.openSafDocument(first.uri, first.mimeType)
                        respondAndLog(
                            category = "Files",
                            command = rawText,
                            reply = "Found ${matches.size} file(s) matching \"${parsed.query}\". Opened \"${first.displayName}\".",
                            status = "Executed",
                            badge = "FILE FOUND",
                            speak = speakResult
                        )
                    } else {
                        respondAndLog(
                            category = "Files",
                            command = rawText,
                            reply = "No files matching \"${parsed.query}\" in your linked SAF folder.",
                            status = "Not Found",
                            badge = "FILE SEARCH",
                            speak = speakResult
                        )
                    }
                }
            }

            is ParsedCommand.DeleteFile -> {
                val treeUri = currentSettings.safFolderUri
                if (treeUri.isBlank()) {
                    _uiEvents.tryEmit(RaniUiEvent.LaunchSafFolderPicker)
                    respondAndLog(
                        category = "Files",
                        command = rawText,
                        reply = "Select a folder via Android Folder Picker (SAF) first to manage or safely delete files.",
                        status = "Needs Folder",
                        badge = "SAF FOLDER",
                        speak = speakResult
                    )
                } else {
                    val match = deviceController.searchSafFiles(treeUri, parsed.query).firstOrNull()
                    if (match != null) {
                        _pendingConfirmation.value = ConfirmationRequest.DeleteFile(
                            fileUri = match.uri,
                            fileName = match.displayName
                        )
                        respondAndLog(
                            category = "Files",
                            command = rawText,
                            reply = "Confirmation requested before deleting \"${match.displayName}\".",
                            status = "Awaiting Confirm",
                            badge = "CONFIRM DELETE",
                            speak = speakResult
                        )
                    } else {
                        respondAndLog(
                            category = "Files",
                            command = rawText,
                            reply = "No file matching \"${parsed.query}\" found in your selected SAF folder.",
                            status = "Not Found",
                            badge = "DELETE FILE",
                            speak = speakResult
                        )
                    }
                }
            }

            is ParsedCommand.AiSummarize -> {
                val source = parsed.textOrEmpty.ifBlank { deviceController.readClipboardText() }
                if (source.isBlank()) {
                    respondAndLog(
                        category = "AI Summarize",
                        command = rawText,
                        reply = "Clipboard is empty. Copy any article or message first, then say \"Summarize\".",
                        status = "Empty Input",
                        badge = "CLIPBOARD EMPTY",
                        speak = speakResult
                    )
                } else {
                    val res = aiRepo.summarizeText(source, currentSettings.language)
                    respondAndLog(
                        category = "AI Summarize",
                        command = rawText,
                        reply = res.text,
                        status = "Placeholder AI",
                        badge = "AI SUMMARY",
                        isPlaceholderAi = res.isPlaceholder,
                        speak = speakResult
                    )
                }
            }

            is ParsedCommand.AiTranslate -> {
                val res = aiRepo.translateText(parsed.text, parsed.targetLanguage)
                respondAndLog(
                    category = "AI Translate",
                    command = rawText,
                    reply = res.text,
                    status = "Placeholder AI",
                    badge = "TRANSLATE",
                    isPlaceholderAi = res.isPlaceholder,
                    speak = speakResult
                )
            }

            is ParsedCommand.AiGenerateImage -> {
                val preview = aiRepo.generateImagePreview(parsed.prompt)
                _latestImagePreview.value = preview
                respondAndLog(
                    category = "AI Image",
                    command = rawText,
                    reply = "${preview.summary}\n${preview.notice}",
                    status = "Placeholder AI",
                    badge = "IMAGE GEN",
                    isPlaceholderAi = true,
                    speak = speakResult
                )
            }

            is ParsedCommand.AiCodingOrEdu -> {
                val res = aiRepo.helpWithCodingOrEducation(parsed.prompt, currentSettings.language)
                respondAndLog(
                    category = "AI Tutor",
                    command = rawText,
                    reply = res.text,
                    status = "Placeholder AI",
                    badge = "AI STUDY/CODE",
                    isPlaceholderAi = res.isPlaceholder,
                    speak = speakResult
                )
            }

            is ParsedCommand.AiGeneralQuery -> {
                val memNotes = memories.value.map { "${it.prefKey}: ${it.prefValue}" }
                val res = aiRepo.askAssistant(parsed.query, currentSettings.language, memNotes)
                respondAndLog(
                    category = "AI Assistant",
                    command = rawText,
                    reply = res.text,
                    status = "Placeholder AI",
                    badge = "SAMPLE AI",
                    isPlaceholderAi = res.isPlaceholder,
                    speak = speakResult
                )
            }
        }
    }

    fun confirmPendingAction() {
        val req = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            when (req) {
                is ConfirmationRequest.PlaceCall -> {
                    if (req.rawTarget.startsWith("whatsapp:")) {
                        val contact = req.rawTarget.removePrefix("whatsapp:")
                        val res = deviceController.openWhatsAppChat(contact)
                        respondActionResult("WhatsApp Call", "Call $contact", res, "WHATSAPP CALL", true)
                    } else {
                        val res = deviceController.placePhoneCall(req.rawTarget)
                        respondActionResult("Phone Call", "Call ${req.targetDisplay}", res, "CALL PLACED", true)
                    }
                }

                is ConfirmationRequest.SendWhatsAppMessage -> {
                    val res = deviceController.openWhatsAppChat(req.recipient, req.messageText)
                    respondActionResult("WhatsApp", "Message ${req.recipient}", res, "WHATSAPP READY", true)
                }

                is ConfirmationRequest.ShareMedia -> {
                    val res = deviceController.shareMediaUri(
                        uri = req.uri,
                        mimeType = req.mimeType,
                        caption = if (req.recipientHint.isNotBlank()) "For ${req.recipientHint}" else "",
                        preferWhatsApp = req.preferWhatsApp
                    )
                    respondActionResult("Media Share", "Share file", res, "SHARED", true)
                }

                is ConfirmationRequest.DeleteFile -> {
                    val res = deviceController.deleteSafDocument(req.fileUri, req.fileName)
                    if (res.success) {
                        _safSearchResults.value = _safSearchResults.value.filterNot { it.uri == req.fileUri }
                    }
                    respondActionResult("File Delete", "Delete ${req.fileName}", res, "FILE DELETED", true)
                }

                is ConfirmationRequest.PaymentGuard -> {
                    respondAndLog(
                        category = "Security",
                        command = req.commandText,
                        reply = "Payment guard acknowledged. No automated payment performed.",
                        status = "Protected",
                        badge = "SAFE",
                        speak = false
                    )
                }
            }
        }
    }

    fun dismissPendingAction() {
        val req = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        viewModelScope.launch {
            dao.insertActivityLog(
                ActivityLogEntity(
                    command = req.title,
                    language = settings.value.language.name,
                    category = "Confirmation",
                    resultSummary = "Cancelled by user",
                    status = "Cancelled"
                )
            )
            dao.insertChatMessage(
                ChatMessageEntity(
                    role = "rani",
                    text = "Cancelled: ${req.title}. You stay in control.",
                    actionBadge = "CANCELLED"
                )
            )
        }
    }

    fun onPhotoPicked(uri: Uri) {
        val current = settings.value
        val recipient = pendingPhotoRecipientHint
        pendingPhotoRecipientHint = ""
        if (current.confirmBeforeShare) {
            _pendingConfirmation.value = ConfirmationRequest.ShareMedia(
                uri = uri,
                mimeType = "image/*",
                recipientHint = recipient,
                preferWhatsApp = recipient.isNotBlank()
            )
        } else {
            val res = deviceController.shareMediaUri(uri, "image/*", preferWhatsApp = recipient.isNotBlank())
            viewModelScope.launch {
                respondActionResult("Photo Share", "Share selected photo", res, "PHOTO SHARE", false)
            }
        }
    }

    fun onDocumentPickedForShare(uri: Uri) {
        _pendingConfirmation.value = ConfirmationRequest.ShareMedia(
            uri = uri,
            mimeType = "*/*",
            recipientHint = "WhatsApp / App",
            preferWhatsApp = true
        )
    }

    fun onSafFolderSelected(uri: Uri) {
        viewModelScope.launch {
            settingsRepo.setSafFolderUri(uri.toString())
            val files = deviceController.searchSafFiles(uri.toString(), "")
            _safSearchResults.value = files
            _uiEvents.tryEmit(RaniUiEvent.ShowSnackbar("Folder linked (${files.size} files indexed safely via SAF)."))
        }
    }

    fun searchContactsNow(query: String) {
        if (!deviceController.hasPermission(Manifest.permission.READ_CONTACTS)) {
            _uiEvents.tryEmit(
                RaniUiEvent.RequestPermission(
                    Manifest.permission.READ_CONTACTS,
                    "Contacts permission lets RANI search names for calls & WhatsApp."
                )
            )
            return
        }
        _contactSearchResults.value = deviceController.searchContacts(query)
    }

    fun searchSafFilesNow(query: String) {
        val uri = settings.value.safFolderUri
        if (uri.isBlank()) {
            _uiEvents.tryEmit(RaniUiEvent.LaunchSafFolderPicker)
            return
        }
        _safSearchResults.value = deviceController.searchSafFiles(uri, query)
    }

    fun requestDeleteSafFile(entry: SafFileEntry) {
        _pendingConfirmation.value = ConfirmationRequest.DeleteFile(
            fileUri = entry.uri,
            fileName = entry.displayName
        )
    }

    fun toggleWakeWordBackground(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (!deviceController.hasPermission(Manifest.permission.RECORD_AUDIO)) {
                    _uiEvents.tryEmit(
                        RaniUiEvent.RequestPermission(
                            Manifest.permission.RECORD_AUDIO,
                            "Microphone permission is required for background Hey Rani listening."
                        )
                    )
                    return@launch
                }
                settingsRepo.setWakeWordBackgroundEnabled(true)
                WakeWordForegroundService.start(getApplication())
            } else {
                settingsRepo.setWakeWordBackgroundEnabled(false)
                WakeWordForegroundService.stop(getApplication())
            }
        }
    }

    fun toggleMicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setMicEnabled(enabled)
            if (!enabled) {
                voiceEngine.stopListening()
                settingsRepo.setWakeWordBackgroundEnabled(false)
                WakeWordForegroundService.stop(getApplication())
            }
        }
    }

    fun toggleAutomationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAutomationEnabled(enabled) }
    }

    fun toggleContinuousMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setContinuousConversation(enabled) }
    }

    fun setLanguage(language: AssistantLanguage) {
        viewModelScope.launch { settingsRepo.setLanguage(language) }
    }

    fun updateTtsParams(rate: Float, pitch: Float) {
        viewModelScope.launch { settingsRepo.setTtsParams(rate, pitch) }
    }

    fun updateConfirmationRule(
        confirmMsg: Boolean? = null,
        confirmCall: Boolean? = null,
        confirmShare: Boolean? = null,
        confirmDelete: Boolean? = null
    ) {
        viewModelScope.launch {
            settingsRepo.setConfirmationRule(confirmMsg, confirmCall, confirmShare, confirmDelete)
        }
    }

    fun setPrivacyFilterNotifications(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setPrivacyFilterNotifications(enabled) }
    }

    fun addManualMemory(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            dao.upsertMemory(
                MemoryPreferenceEntity(
                    prefKey = key.trim(),
                    prefValue = value.trim(),
                    category = "User Approved"
                )
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { dao.deleteMemoryById(id) }
    }

    fun clearHistoryAndChat() {
        viewModelScope.launch {
            dao.clearActivityLogs()
            dao.clearChatMessages()
            seedWelcomeIfEmpty()
        }
    }

    fun draftMessageTool(recipient: String, topic: String) {
        viewModelScope.launch {
            val res = aiRepo.draftMessage(recipient, topic, settings.value.language)
            respondAndLog(
                category = "AI Draft",
                command = "Draft message to $recipient",
                reply = res.text,
                status = "Placeholder AI",
                badge = "AI DRAFT",
                isPlaceholderAi = res.isPlaceholder,
                speak = false
            )
            _pendingConfirmation.value = ConfirmationRequest.SendWhatsAppMessage(
                recipient = recipient.ifBlank { "Contact" },
                messageText = res.text
            )
        }
    }

    private suspend fun notifyAutomationDisabled(rawText: String) {
        respondAndLog(
            category = "Privacy",
            command = rawText,
            reply = "Phone Automation is currently disabled in Access tab privacy controls.",
            status = "Blocked",
            badge = "AUTOMATION OFF",
            speak = false
        )
    }

    private suspend fun respondActionResult(
        category: String,
        command: String,
        result: ActionExecutionResult,
        badge: String,
        speak: Boolean
    ) {
        if (result.needsPermission != null) {
            _uiEvents.tryEmit(RaniUiEvent.RequestPermission(result.needsPermission, result.message))
        }
        respondAndLog(
            category = category,
            command = command,
            reply = result.message,
            status = if (result.success) "Executed" else "Attention",
            badge = badge,
            speak = speak
        )
    }

    private suspend fun respondAndLog(
        category: String,
        command: String,
        reply: String,
        status: String,
        badge: String,
        isPlaceholderAi: Boolean = false,
        speak: Boolean
    ) {
        dao.insertActivityLog(
            ActivityLogEntity(
                command = command,
                language = settings.value.language.name,
                category = category,
                resultSummary = reply.take(180),
                status = status
            )
        )
        dao.insertChatMessage(
            ChatMessageEntity(
                role = "rani",
                text = reply,
                isPlaceholderAi = isPlaceholderAi,
                actionBadge = badge
            )
        )
        if (speak) {
            voiceEngine.speak(reply)
        } else {
            voiceEngine.setIdle()
        }
    }

    override fun onCleared() {
        voiceEngine.release()
        super.onCleared()
    }
}
