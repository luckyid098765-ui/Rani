package com.example.domain

import android.net.Uri

sealed interface ConfirmationRequest {
    val title: String
    val subtitle: String
    val confirmLabel: String
    val isHighRisk: Boolean

    data class PlaceCall(
        val targetDisplay: String,
        val rawTarget: String,
        override val title: String = "Confirm Phone Call",
        override val subtitle: String = "Place call to $targetDisplay?",
        override val confirmLabel: String = "Call Now",
        override val isHighRisk: Boolean = false
    ) : ConfirmationRequest

    data class SendWhatsAppMessage(
        val recipient: String,
        val messageText: String,
        override val title: String = "Confirm WhatsApp Message",
        override val subtitle: String = "Prepare message to $recipient:\n\"$messageText\"",
        override val confirmLabel: String = "Open & Send",
        override val isHighRisk: Boolean = false
    ) : ConfirmationRequest

    data class ShareMedia(
        val uri: Uri,
        val mimeType: String,
        val recipientHint: String,
        val preferWhatsApp: Boolean,
        override val title: String = "Confirm Media Share",
        override val subtitle: String = "Share selected photo/document${if (recipientHint.isNotBlank()) " with $recipientHint" else ""}?",
        override val confirmLabel: String = "Share Now",
        override val isHighRisk: Boolean = false
    ) : ConfirmationRequest

    data class DeleteFile(
        val fileUri: Uri,
        val fileName: String,
        override val title: String = "Confirm File Deletion",
        override val subtitle: String = "Permanently delete \"$fileName\" from your selected folder?",
        override val confirmLabel: String = "Delete File",
        override val isHighRisk: Boolean = true
    ) : ConfirmationRequest

    data class PaymentGuard(
        val commandText: String,
        override val title: String = "Payment Protection Alert",
        override val subtitle: String = "RANI AI never executes payments automatically. Open your payment app manually if you want to proceed with: \"$commandText\".",
        override val confirmLabel: String = "Understood",
        override val isHighRisk: Boolean = true
    ) : ConfirmationRequest
}
