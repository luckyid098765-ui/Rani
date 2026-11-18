package com.example.domain

import com.example.phone.DeviceController.SupportedApp

sealed interface ParsedCommand {
    data class OpenApp(val app: SupportedApp) : ParsedCommand
    data class SearchYouTube(val query: String) : ParsedCommand
    data class SearchWeb(val query: String) : ParsedCommand
    data class OpenWhatsAppChat(val chatName: String) : ParsedCommand
    data class SendWhatsAppMessage(val recipient: String, val message: String) : ParsedCommand
    data class WhatsAppCall(val contact: String) : ParsedCommand
    data class ShareLatestPhoto(val recipient: String) : ParsedCommand
    data class CallContact(val target: String) : ParsedCommand
    data object OpenCallHistory : ParsedCommand
    data object AnswerCall : ParsedCommand
    data object RejectCall : ParsedCommand
    data class ScrollFeed(val forward: Boolean) : ParsedCommand
    data class TapAppControl(val controlName: String, val keywords: List<String>) : ParsedCommand
    data class TypeText(val text: String) : ParsedCommand
    data object ReadVisibleScreen : ParsedCommand
    data object ReadNotifications : ParsedCommand
    data object GlobalBack : ParsedCommand
    data object GlobalHome : ParsedCommand
    data class VolumeAdjust(val direction: Int) : ParsedCommand
    data class BrightnessAdjust(val increase: Boolean) : ParsedCommand
    data class MediaControl(val action: String) : ParsedCommand
    data class SetAlarm(val hour: Int, val minute: Int) : ParsedCommand
    data class SetTimer(val seconds: Int) : ParsedCommand
    data class OpenSettings(val section: String) : ParsedCommand
    data class FindFile(val query: String) : ParsedCommand
    data class DeleteFile(val query: String) : ParsedCommand
    data class RememberPreference(val key: String, val value: String) : ParsedCommand
    data class AiSummarize(val textOrEmpty: String) : ParsedCommand
    data class AiTranslate(val text: String, val targetLanguage: String) : ParsedCommand
    data class AiGenerateImage(val prompt: String) : ParsedCommand
    data class AiCodingOrEdu(val prompt: String) : ParsedCommand
    data class PaymentAttempt(val rawCommand: String) : ParsedCommand
    data class AiGeneralQuery(val query: String) : ParsedCommand
}

object MultilingualCommandParser {

    fun stripWakeWord(raw: String): String {
        val trimmed = raw.trim()
        val prefixes = listOf(
            "hey rani,", "hey rani", "ok rani", "hi rani", "rani,", "rani",
            "हे रानी", "रानी,", "रानी", "सुनो रानी"
        )
        val lower = trimmed.lowercase()
        for (p in prefixes) {
            if (lower.startsWith(p)) {
                return trimmed.drop(p.length).trimStart(',', ' ', ':', '-')
            }
        }
        return trimmed
    }

    fun parse(rawInput: String): ParsedCommand {
        val cleaned = stripWakeWord(rawInput).ifBlank { rawInput.trim() }
        val lower = cleaned.lowercase()

        // 1. Payment Guard (never perform payments without explicit user action)
        if (containsAny(lower, listOf("pay ", "send money", "upi", "gpay", "phonepe", "paytm", "पैसे भेजो", "पेमेंट"))) {
            return ParsedCommand.PaymentAttempt(cleaned)
        }

        // 2. Remember user preference ("remember that I like Hindi" / "yaad rakho ...")
        if (lower.startsWith("remember ") || lower.startsWith("yaad rakho ") || lower.contains("याद रखो")) {
            val fact = cleaned
                .removePrefix("remember that ").removePrefix("remember ")
                .removePrefix("yaad rakho ").removePrefix("याद रखो ")
                .trim()
            if (fact.isNotBlank()) {
                return ParsedCommand.RememberPreference(key = "User Note", value = fact)
            }
        }

        // 3. Call controls (Answer / Reject / History / Call)
        if (containsAny(lower, listOf("answer call", "pick up call", "call uthao", "फोन उठाओ"))) {
            return ParsedCommand.AnswerCall
        }
        if (containsAny(lower, listOf("reject call", "end call", "cut call", "phone kaato", "फोन काटो"))) {
            return ParsedCommand.RejectCall
        }
        if (containsAny(lower, listOf("call history", "recent calls", "call log", "कॉल हिस्ट्री"))) {
            return ParsedCommand.OpenCallHistory
        }
        if (containsAny(lower, listOf("whatsapp call", "व्हाट्सएप कॉल"))) {
            val target = extractAfterKeywords(cleaned, listOf("whatsapp call", "व्हाट्सएप कॉल"))
            return ParsedCommand.WhatsAppCall(target.ifBlank { "contact" })
        }
        if (lower.startsWith("call ") || lower.startsWith("dial ") ||
            lower.contains("ko call karo") || lower.contains("ko phone lagao") || lower.contains("कॉल करो")
        ) {
            val target = cleaned
                .replace(Regex("^(call|dial)\\s+", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*(ko call karo|ko phone lagao|कॉल करो|फोन लगाओ).*$", RegexOption.IGNORE_CASE), "")
                .trim()
            if (target.isNotBlank()) return ParsedCommand.CallContact(target)
        }

        // 4. WhatsApp Open Chat & Photo Share & Send Message
        if (lower.contains("open chat") || lower.contains("chat kholo")) {
            val chat = extractAfterKeywords(cleaned, listOf("open chat", "chat kholo"))
            return ParsedCommand.OpenWhatsAppChat(chat.ifBlank { "WhatsApp" })
        }
        if ((lower.contains("photo") || lower.contains("फोटो") || lower.contains("image")) &&
            containsAny(lower, listOf("bhejo", "send", "share", "भेजो"))
        ) {
            val recipient = cleaned
                .replace(Regex("(latest|last|recent)?\\s*photo", RegexOption.IGNORE_CASE), "")
                .replace(Regex("(ko bhejo|to|send|share|भेजो|को)", RegexOption.IGNORE_CASE), "")
                .trim()
            return ParsedCommand.ShareLatestPhoto(recipient)
        }
        if (containsAny(lower, listOf("whatsapp message", "send message", "message bhejo", "मैसेज भेजो"))) {
            val parts = cleaned.split(Regex("\\s+(saying|that|likho|लिखो|:)\\s+", RegexOption.IGNORE_CASE), limit = 2)
            val recipientPart = parts.firstOrNull().orEmpty()
                .replace(Regex("(send|whatsapp|message|bhejo|ko|to|मैसेज|भेजो|को)", RegexOption.IGNORE_CASE), "")
                .trim()
            val bodyPart = parts.getOrNull(1)?.trim().orEmpty()
            return ParsedCommand.SendWhatsAppMessage(
                recipient = recipientPart.ifBlank { "Contact" },
                message = bodyPart.ifBlank { "Hello!" }
            )
        }

        // 5. File Search & Safe Delete
        if (containsAny(lower, listOf("find file", "search file", "file dhundo", "फाइल ढूंढो", "फाइल खोजो"))) {
            val q = extractAfterKeywords(cleaned, listOf("find file", "search file", "file dhundo", "फाइल ढूंढो", "फाइल खोजो"))
            return ParsedCommand.FindFile(q)
        }
        if (containsAny(lower, listOf("delete file", "remove file", "फाइल डिलीट", "file delete"))) {
            val q = extractAfterKeywords(cleaned, listOf("delete file", "remove file", "फाइल डिलीट", "file delete"))
            return ParsedCommand.DeleteFile(q)
        }

        // 6. Accessibility In-App Social & Navigation Commands
        if (containsAny(lower, listOf("scroll reel", "next reel", "next short", "scroll down", "agla reel", "अगला रील", "नीचे स्क्रॉल"))) {
            return ParsedCommand.ScrollFeed(forward = true)
        }
        if (containsAny(lower, listOf("previous reel", "scroll up", "pichla reel", "पिछला रील", "ऊपर स्क्रॉल"))) {
            return ParsedCommand.ScrollFeed(forward = false)
        }
        if (lower.startsWith("type ") || lower.startsWith("likho ") || lower.startsWith("लिखो ")) {
            val txt = extractAfterKeywords(cleaned, listOf("type", "likho", "लिखो"))
            if (txt.isNotBlank()) return ParsedCommand.TypeText(txt)
        }
        if (containsAny(lower, listOf("like video", "like post", "like reel", "like karo", "लाइक करो")) || lower == "like") {
            return ParsedCommand.TapAppControl("Like", listOf("like", "पसंद"))
        }
        if (containsAny(lower, listOf("comment", "कमेंट"))) {
            return ParsedCommand.TapAppControl("Comment", listOf("comment", "add a comment", "टिप्पणी"))
        }
        if (containsAny(lower, listOf("follow", "फॉलो"))) {
            return ParsedCommand.TapAppControl("Follow", listOf("follow", "फॉलो"))
        }
        if (containsAny(lower, listOf("subscribe", "सब्सक्राइब"))) {
            return ParsedCommand.TapAppControl("Subscribe", listOf("subscribe", "सदस्यता"))
        }
        if (lower == "share" || lower.contains("share reel") || lower.contains("share post") || lower.contains("शेयर करो")) {
            return ParsedCommand.TapAppControl("Share", listOf("share", "शेयर"))
        }
        if (containsAny(lower, listOf("read screen", "screen padho", "स्क्रीन पढ़ो"))) {
            return ParsedCommand.ReadVisibleScreen
        }
        if (containsAny(lower, listOf("read notification", "notifications", "मैसेज पढ़ो", "नोटिफिकेशन"))) {
            return ParsedCommand.ReadNotifications
        }
        if (lower == "back" || lower == "go back" || lower == "पीछे जाओ") {
            return ParsedCommand.GlobalBack
        }
        if (lower == "home" || lower == "go home" || lower == "होम स्क्रीन") {
            return ParsedCommand.GlobalHome
        }

        // 7. Device Volume, Brightness, Music, Alarm, Settings
        if (containsAny(lower, listOf("volume up", "increase volume", "awaaz badhao", "आवाज़ बढ़ाओ"))) {
            return ParsedCommand.VolumeAdjust(android.media.AudioManager.ADJUST_RAISE)
        }
        if (containsAny(lower, listOf("volume down", "lower volume", "awaaz kam", "आवाज़ कम"))) {
            return ParsedCommand.VolumeAdjust(android.media.AudioManager.ADJUST_LOWER)
        }
        if (containsAny(lower, listOf("mute", "silent", "चुप"))) {
            return ParsedCommand.VolumeAdjust(android.media.AudioManager.ADJUST_MUTE)
        }
        if (containsAny(lower, listOf("brightness up", "increase brightness", "brightness badhao", "रोशनी बढ़ाओ"))) {
            return ParsedCommand.BrightnessAdjust(true)
        }
        if (containsAny(lower, listOf("brightness down", "decrease brightness", "brightness kam", "रोशनी कम"))) {
            return ParsedCommand.BrightnessAdjust(false)
        }
        if (containsAny(lower, listOf("pause music", "stop music", "play music", "resume music", "गाना रोको", "गाना चलाओ"))) {
            return ParsedCommand.MediaControl("play_pause")
        }
        if (containsAny(lower, listOf("next song", "next track", "agla gana", "अगला गाना"))) {
            return ParsedCommand.MediaControl("next")
        }
        if (containsAny(lower, listOf("previous song", "pichla gana", "पिछला गाना"))) {
            return ParsedCommand.MediaControl("previous")
        }
        if (containsAny(lower, listOf("set alarm", "alarm lagao", "अलार्म लगाओ"))) {
            val timeMatch = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(lower)
            var hour = timeMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 7
            val minute = timeMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
            val amPm = timeMatch?.groupValues?.getOrNull(3)?.lowercase().orEmpty()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return ParsedCommand.SetAlarm(hour, minute)
        }
        if (containsAny(lower, listOf("set timer", "timer lagao", "टाइमर"))) {
            val num = Regex("(\\d+)").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 5
            val seconds = if (lower.contains("second")) num else num * 60
            return ParsedCommand.SetTimer(seconds)
        }
        if (containsAny(lower, listOf("open settings", "settings kholo", "सेटिंग्स खोलो", "wifi settings", "bluetooth settings"))) {
            val section = when {
                lower.contains("wifi") -> "wifi"
                lower.contains("bluetooth") -> "bluetooth"
                lower.contains("accessibility") -> "accessibility"
                else -> "main"
            }
            return ParsedCommand.OpenSettings(section)
        }

        // 8. YouTube / Chrome Search & Supported Apps Launch
        if ((lower.contains("youtube") || lower.contains("यूट्यूब")) &&
            containsAny(lower, listOf("search", "play", "find", "खोजो", "चलाओ"))
        ) {
            val q = cleaned
                .replace(Regex("(on youtube|youtube par|youtube mein|search|play|find|youtube|खोजो|चलाओ|यूट्यूब)", RegexOption.IGNORE_CASE), "")
                .trim()
            if (q.isNotBlank()) return ParsedCommand.SearchYouTube(q)
        }
        if (containsAny(lower, listOf("search google", "search chrome", "google karo", "search web", "गूगल करो"))) {
            val q = extractAfterKeywords(cleaned, listOf("search google for", "search google", "search chrome for", "search chrome", "google karo", "search web", "गूगल करो"))
            if (q.isNotBlank()) return ParsedCommand.SearchWeb(q)
        }

        val appMatch = when {
            lower.contains("youtube") || lower.contains("यूट्यूब") -> SupportedApp.YOUTUBE
            lower.contains("whatsapp") || lower.contains("व्हाट्सएप") -> SupportedApp.WHATSAPP
            lower.contains("instagram") || lower.contains("इंस्टाग्राम") -> SupportedApp.INSTAGRAM
            lower.contains("facebook") || lower.contains("फेसबुक") -> SupportedApp.FACEBOOK
            lower.contains("telegram") || lower.contains("टेलीग्राम") -> SupportedApp.TELEGRAM
            lower.contains("chrome") || lower.contains("क्रोम") -> SupportedApp.CHROME
            else -> null
        }
        if (appMatch != null && containsAny(lower, listOf("open", "launch", "start", "kholo", "chalao", "खोलो", "चलाओ"))) {
            return ParsedCommand.OpenApp(appMatch)
        }

        // 9. AI Tasks (Summarize, Translate, Generate Image, Coding/Edu, General Q&A)
        if (containsAny(lower, listOf("summarize", "summary", "सारांश"))) {
            val extra = extractAfterKeywords(cleaned, listOf("summarize", "summary of", "सारांश"))
            return ParsedCommand.AiSummarize(extra)
        }
        if (containsAny(lower, listOf("translate", "अनुवाद"))) {
            val target = when {
                lower.contains("hindi") -> "Hindi"
                lower.contains("english") -> "English"
                lower.contains("spanish") -> "Spanish"
                else -> "Hindi"
            }
            val text = cleaned
                .replace(Regex("(translate|into|to|in|hindi|english|spanish|अनुवाद)", RegexOption.IGNORE_CASE), "")
                .trim()
            return ParsedCommand.AiTranslate(text.ifBlank { cleaned }, target)
        }
        if (containsAny(lower, listOf("generate image", "create image", "make picture", "इमेज बनाओ", "तस्वीर बनाओ"))) {
            val prompt = extractAfterKeywords(cleaned, listOf("generate image of", "generate image", "create image of", "create image", "make picture of", "इमेज बनाओ", "तस्वीर बनाओ"))
            return ParsedCommand.AiGenerateImage(prompt.ifBlank { cleaned })
        }
        if (containsAny(lower, listOf("code", "kotlin", "python", "java", "homework", "math", "explain", "कोड", "समझाओ"))) {
            return ParsedCommand.AiCodingOrEdu(cleaned)
        }

        return ParsedCommand.AiGeneralQuery(cleaned)
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it) }

    private fun extractAfterKeywords(text: String, keywords: List<String>): String {
        val lower = text.lowercase()
        for (kw in keywords) {
            val idx = lower.indexOf(kw.lowercase())
            if (idx >= 0) {
                return text.substring(idx + kw.length).trim(' ', ':', '-', '"')
            }
        }
        return text.trim()
    }
}
