package com.example.data.model

data class SyncProgress(
    val isSyncing: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val statusText: String = "",
    val isCompleted: Boolean = false
) {
    val progress: Float
        get() = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
}
