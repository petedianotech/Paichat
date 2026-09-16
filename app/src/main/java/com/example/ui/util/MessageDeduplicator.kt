package com.example.ui.util

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe deduplication engine to prevent duplicate processing of SMS events,
 * duplicate notifications, and multiple popup instances.
 */
object MessageDeduplicator {
    private const val DEDUPLICATION_WINDOW_MS = 12_000L // 12 seconds deduplication window
    private val processedMessages = ConcurrentHashMap<String, Long>()

    /**
     * Checks if this message fingerprint (sender + content) was seen within the deduplication window.
     * If not seen, records the timestamp and returns false (not a duplicate).
     * If seen, returns true (is a duplicate).
     */
    fun isDuplicateAndMark(sender: String, content: String): Boolean {
        val now = System.currentTimeMillis()
        cleanOldEntries(now)

        val normalizedSender = PhoneNumberUtil.normalize(sender)
        val normalizedContent = content.trim()
        val key = "$normalizedSender|$normalizedContent"

        val lastSeen = processedMessages[key]
        if (lastSeen != null && (now - lastSeen) < DEDUPLICATION_WINDOW_MS) {
            return true
        }

        processedMessages[key] = now
        return false
    }

    private fun cleanOldEntries(now: Long) {
        val iterator = processedMessages.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > DEDUPLICATION_WINDOW_MS * 2) {
                iterator.remove()
            }
        }
    }
}

/**
 * Tracks the currently open / focused conversation in the main chat screen.
 * Used to suppress redundant floating popups when the user is already actively chatting with that contact.
 */
object ActiveConversationTracker {
    @Volatile
    var activeConversationId: String? = null
}
