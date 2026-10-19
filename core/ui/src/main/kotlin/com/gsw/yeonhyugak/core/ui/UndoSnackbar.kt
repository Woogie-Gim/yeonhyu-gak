package com.gsw.yeonhyugak.core.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.platform.AccessibilityManager
import kotlinx.coroutines.withTimeoutOrNull

const val UNDO_MS = 5_000L

// Undo 스낵바 5초. TalkBack 사용자는 권장 시간만큼 더 길게. 되돌리기를 누르면 true
suspend fun SnackbarHostState.showUndo(
    message: String,
    undoLabel: String,
    a11y: AccessibilityManager?,
): Boolean {
    val timeout = a11y?.calculateRecommendedTimeoutMillis(
        UNDO_MS,
        containsIcons = false,
        containsText = true,
        containsControls = true,
    ) ?: UNDO_MS
    currentSnackbarData?.dismiss()
    val result = withTimeoutOrNull(timeout) {
        showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Indefinite,
        )
    }
    return result == SnackbarResult.ActionPerformed
}
