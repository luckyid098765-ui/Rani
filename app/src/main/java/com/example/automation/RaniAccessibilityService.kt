package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RaniAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        Log.i(TAG, "RaniAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()
        if (!pkg.isNullOrBlank()) {
            _activePackage.value = pkg
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "RaniAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isConnected.value = false
    }

    fun performScroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findFirstScrollable(root)
            if (scrollable != null) {
                val action = if (forward) {
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                } else {
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                }
                if (scrollable.performAction(action)) {
                    return true
                }
            }
        }
        return performSwipeGesture(forward)
    }

    private fun performSwipeGesture(upward: Boolean): Boolean {
        val metrics = resources.displayMetrics
        val centerX = metrics.widthPixels * 0.5f
        val startY = if (upward) metrics.heightPixels * 0.76f else metrics.heightPixels * 0.28f
        val endY = if (upward) metrics.heightPixels * 0.24f else metrics.heightPixels * 0.76f
        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 280L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    fun clickMatchingKeywords(keywords: List<String>): String? {
        val root = rootInActiveWindow ?: return null
        val target = findNodeByKeywords(root, keywords) ?: return null
        val clickable = findClickableAncestor(target) ?: target
        val clicked = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return if (clicked) {
            (target.text ?: target.contentDescription ?: keywords.first()).toString()
        } else {
            val bounds = Rect()
            clickable.getBoundsInScreen(bounds)
            if (!bounds.isEmpty && tapPoint(bounds.exactCenterX(), bounds.exactCenterY())) {
                keywords.first()
            } else {
                null
            }
        }
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val target = if (focused != null && focused.isEditable && !focused.isPassword) {
            focused
        } else {
            findFirstEditableNonPassword(root)
        } ?: return false

        target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun searchAndOpenInApp(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        // Try clicking a search icon/button first if no editable field is visible
        var editNode = findFirstEditableNonPassword(root)
        if (editNode == null) {
            val searchClicked = clickMatchingKeywords(listOf("search", "खोजें", "find"))
            if (searchClicked != null) {
                editNode = findFirstEditableNonPassword(rootInActiveWindow ?: root)
            }
        }
        if (editNode != null) {
            editNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
            }
            val setOk = editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (setOk) {
                // Attempt to click a matching result item if present
                val match = findNodeByKeywords(rootInActiveWindow ?: root, listOf(query))
                if (match != null && match != editNode) {
                    val clickable = findClickableAncestor(match) ?: match
                    clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                return true
            }
        }
        return false
    }

    fun readSafeVisibleText(): String {
        val root = rootInActiveWindow ?: return "No active screen content available."
        val collected = linkedSetOf<String>()
        collectSafeText(root, collected, maxItems = 18)
        return if (collected.isEmpty()) {
            "No readable non-sensitive text found on the current screen."
        } else {
            collected.joinToString(" • ")
        }
    }

    fun runGlobalNavigation(action: Int): Boolean = performGlobalAction(action)

    private fun tapPoint(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findFirstScrollable(child)
            if (match != null) return match
        }
        return null
    }

    private fun findFirstEditableNonPassword(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Security invariant: NEVER read or type into password fields
        if (node.isEditable && !node.isPassword) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findFirstEditableNonPassword(child)
            if (match != null) return match
        }
        return null
    }

    private fun findNodeByKeywords(
        node: AccessibilityNodeInfo,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        if (node.isPassword) return null
        val text = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        val viewId = node.viewIdResourceName?.lowercase().orEmpty()
        for (kw in keywords) {
            val k = kw.lowercase()
            if (text.contains(k) || desc.contains(k) || viewId.contains(k)) {
                return node
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByKeywords(child, keywords)
            if (found != null) return found
        }
        return null
    }

    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 6) {
            if (current.isClickable) return current
            current = current.parent
            depth++
        }
        return null
    }

    private fun collectSafeText(
        node: AccessibilityNodeInfo,
        out: MutableSet<String>,
        maxItems: Int
    ) {
        if (out.size >= maxItems) return
        // Privacy guard: never inspect password or sensitive credential nodes
        if (node.isPassword) return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank() && text.length > 1 && !looksSensitive(text)) {
            out.add(text.take(80))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectSafeText(child, out, maxItems)
            if (out.size >= maxItems) break
        }
    }

    private fun looksSensitive(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("password") ||
            lower.contains("passcode") ||
            lower.contains("cvv") ||
            lower.contains("pin") && lower.contains("enter")
    }

    companion object {
        private const val TAG = "RaniAccessibility"

        @Volatile
        var instance: RaniAccessibilityService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _activePackage = MutableStateFlow("")
        val activePackage: StateFlow<String> = _activePackage.asStateFlow()
    }
}
