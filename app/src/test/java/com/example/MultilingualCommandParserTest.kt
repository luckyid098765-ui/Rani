package com.example

import com.example.domain.MultilingualCommandParser
import com.example.domain.ParsedCommand
import com.example.phone.DeviceController.SupportedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultilingualCommandParserTest {

    @Test
    fun parsesWakeWordAndHindiOpenApp() {
        val result = MultilingualCommandParser.parse("Hey Rani YouTube kholo")
        assertTrue(result is ParsedCommand.OpenApp)
        assertEquals(SupportedApp.YOUTUBE, (result as ParsedCommand.OpenApp).app)
    }

    @Test
    fun parsesWhatsAppOpenChatAndPhotoShare() {
        val chat = MultilingualCommandParser.parse("open chat Family")
        assertEquals(ParsedCommand.OpenWhatsAppChat("Family"), chat)

        val photo = MultilingualCommandParser.parse("latest photo Rahul ko bhejo")
        assertTrue(photo is ParsedCommand.ShareLatestPhoto)
        assertEquals("Rahul", (photo as ParsedCommand.ShareLatestPhoto).recipient)
    }

    @Test
    fun parsesFileSearchDeleteAndPaymentGuard() {
        val find = MultilingualCommandParser.parse("find file invoice")
        assertEquals(ParsedCommand.FindFile("invoice"), find)

        val del = MultilingualCommandParser.parse("delete file old_notes.txt")
        assertEquals(ParsedCommand.DeleteFile("old_notes.txt"), del)

        val pay = MultilingualCommandParser.parse("UPI se 500 pay karo")
        assertTrue(pay is ParsedCommand.PaymentAttempt)
    }

    @Test
    fun parsesAccessibilityAndAiSummarize() {
        assertEquals(ParsedCommand.ScrollFeed(true), MultilingualCommandParser.parse("Instagram scroll reels"))
        assertEquals(ParsedCommand.TypeText("hello"), MultilingualCommandParser.parse("type hello"))
        assertTrue(MultilingualCommandParser.parse("summarize") is ParsedCommand.AiSummarize)
    }
}
