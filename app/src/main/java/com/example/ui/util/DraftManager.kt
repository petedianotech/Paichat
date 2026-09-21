package com.example.ui.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DraftManager {
    private const val PREFS_NAME = "paichat_drafts_prefs"
    private var prefs: SharedPreferences? = null
    private val _drafts = MutableStateFlow<Map<String, String>>(emptyMap())
    val drafts: StateFlow<Map<String, String>> = _drafts.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadAllDrafts()
        }
    }

    private fun ensurePrefs(): SharedPreferences? {
        if (prefs == null) {
            try {
                val ctx = com.example.PulseChatApp.getInstance().applicationContext
                prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                loadAllDrafts()
            } catch (_: Exception) {
            }
        }
        return prefs
    }

    private fun loadAllDrafts() {
        val sp = prefs ?: return
        val map = mutableMapOf<String, String>()
        for ((key, value) in sp.all) {
            if (value is String && value.isNotBlank()) {
                map[key] = value
            }
        }
        _drafts.value = map
    }

    fun getDraft(conversationId: String): String {
        ensurePrefs()
        return _drafts.value[conversationId] ?: ""
    }

    fun saveDraft(conversationId: String, text: String) {
        val sp = ensurePrefs() ?: return
        val trimmed = text.trim()
        val current = _drafts.value.toMutableMap()
        if (trimmed.isEmpty()) {
            sp.edit().remove(conversationId).apply()
            current.remove(conversationId)
        } else {
            sp.edit().putString(conversationId, text).apply()
            current[conversationId] = text
        }
        _drafts.value = current
    }

    fun clearDraft(conversationId: String) {
        saveDraft(conversationId, "")
    }
}
