package com.example.data.ai

import com.example.data.preferences.AssistantLanguage
import kotlinx.coroutines.delay

data class AiResponse(
    val text: String,
    val isPlaceholder: Boolean = true,
    val notice: String = "Sample AI response: Live Gemini responses & image generation require Firebase AI Logic to be enabled."
)

data class AiImagePreview(
    val prompt: String,
    val summary: String,
    val isPlaceholder: Boolean = true,
    val notice: String = "Image generation preview: Live Gemini Nano Banana 2.1 requires Firebase AI Logic (Blaze plan) to be enabled."
)

interface AiRepository {
    suspend fun askAssistant(
        query: String,
        language: AssistantLanguage,
        rememberedNotes: List<String> = emptyList()
    ): AiResponse

    suspend fun draftMessage(
        recipient: String,
        topic: String,
        language: AssistantLanguage
    ): AiResponse

    suspend fun translateText(
        text: String,
        targetLanguage: String
    ): AiResponse

    suspend fun summarizeText(
        text: String,
        language: AssistantLanguage
    ): AiResponse

    suspend fun helpWithCodingOrEducation(
        prompt: String,
        language: AssistantLanguage
    ): AiResponse

    suspend fun generateImagePreview(prompt: String): AiImagePreview
}

/**
 * Placeholder implementation used because Firebase AI Logic setup was declined.
 * All phone automation, voice recognition, TTS, calling, WhatsApp, Accessibility,
 * and SAF file actions run natively on-device without needing cloud AI.
 */
class PlaceholderAiRepository : AiRepository {

    override suspend fun askAssistant(
        query: String,
        language: AssistantLanguage,
        rememberedNotes: List<String>
    ): AiResponse {
        delay(350)
        val memoryHint = if (rememberedNotes.isNotEmpty()) {
            "\n\nSaved preferences in mind: ${rememberedNotes.take(2).joinToString(" • ")}"
        } else ""
        val reply = when (language) {
            AssistantLanguage.HINDI ->
                "नमस्ते! यह एक सैंपल AI उत्तर है (\"$query\")। आपका फ़ोन ऑटोमेशन, कॉलिंग, व्हाट्सऐप और आवाज़ कमांड अभी पूरी तरह काम कर रहे हैं।$memoryHint"
            AssistantLanguage.HINGLISH ->
                "Namaste! Yeh ek sample AI response hai aapke sawal \"$query\" ke liye. Phone actions, WhatsApp, calls aur voice controls 100% live hain!$memoryHint"
            else ->
                "Sample AI response for \"$query\": Phone automation, voice control, calling, WhatsApp, and accessibility actions work live on-device.$memoryHint"
        }
        return AiResponse(text = reply, isPlaceholder = true)
    }

    override suspend fun draftMessage(
        recipient: String,
        topic: String,
        language: AssistantLanguage
    ): AiResponse {
        delay(300)
        val cleanRecipient = recipient.ifBlank { "Friend" }
        val cleanTopic = topic.ifBlank { "catching up" }
        val draft = when (language) {
            AssistantLanguage.HINDI ->
                "नमस्ते $cleanRecipient, आशा है आप ठीक होंगे। मुझे आपसे $cleanTopic के बारे में बात करनी थी। कृपया समय मिलने पर बताएं।"
            AssistantLanguage.HINGLISH ->
                "Hi $cleanRecipient! Hope you're doing well. Mujhe aapse $cleanTopic ke baare mein baat karni thi. Free hokar reply karna!"
            else ->
                "Hi $cleanRecipient, hope you're doing well! I wanted to reach out regarding $cleanTopic. Let me know when you're free to connect."
        }
        return AiResponse(text = draft, isPlaceholder = true)
    }

    override suspend fun translateText(text: String, targetLanguage: String): AiResponse {
        delay(250)
        return AiResponse(
            text = "[Sample Translation -> $targetLanguage]\nSource: \"$text\"\nEnable Firebase AI Logic to unlock live neural translation across 100+ languages.",
            isPlaceholder = true
        )
    }

    override suspend fun summarizeText(text: String, language: AssistantLanguage): AiResponse {
        delay(300)
        val preview = text.trim().take(180)
        val wordCount = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val summary = if (language == AssistantLanguage.HINDI) {
            "सैंपल सारांश ($wordCount शब्द): \"$preview${if (text.length > 180) "..." else ""}\""
        } else {
            "Sample Summary ($wordCount words captured):\n• Key snippet: \"$preview${if (text.length > 180) "..." else ""}\"\n• Status: Ready on-device; connect Firebase AI Logic for deep Gemini summarization."
        }
        return AiResponse(text = summary, isPlaceholder = true)
    }

    override suspend fun helpWithCodingOrEducation(
        prompt: String,
        language: AssistantLanguage
    ): AiResponse {
        delay(300)
        return AiResponse(
            text = "Sample Study & Coding Assistant for \"$prompt\":\n1. Break the problem into inputs, processing, and edge cases.\n2. Test with small examples first.\n\nConnect Firebase AI Logic to get full Gemini step-by-step code & tutoring.",
            isPlaceholder = true
        )
    }

    override suspend fun generateImagePreview(prompt: String): AiImagePreview {
        delay(350)
        return AiImagePreview(
            prompt = prompt,
            summary = "Prompt queued: \"$prompt\". Live AI image synthesis uses Gemini Nano Banana 2.1 through Firebase AI Logic (Blaze plan).",
            isPlaceholder = true
        )
    }
}
