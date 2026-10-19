package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.IntSize
import com.gsw.yeonhyugak.core.engine.calendar.DayCell
import com.gsw.yeonhyugak.core.engine.calendar.MonthGrid
import java.time.LocalDate

// 제스처 판별: 200ms 안에 떼면 탭, 움직이면 월 넘김, 버티면 길게 누르기
internal const val LONG_PRESS_MS = 200L

private const val DAYS_IN_WEEK = 7

internal enum class PressOutcome { Tap, LongRelease, LongPress, Swipe, Cancel }

// 그리드 전체 제스처. 최신 grid·callbacks는 getter로 읽는다
internal fun Modifier.calendarGestures(
    grid: () -> MonthGrid,
    callbacks: () -> GridCallbacks,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val start = grid().cellAt(down.position, size) ?: return@awaitEachGesture
        val cb = callbacks()
        var last: PointerInputChange? = null
        val outcome = withTimeoutOrNull(LONG_PRESS_MS) {
            var result: PressOutcome? = null
            while (result == null) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                last = change
                result = classify(change, down, viewConfiguration.touchSlop)
            }
            result
        } ?: PressOutcome.LongPress

        when (outcome) {
            PressOutcome.Tap -> cb.onTap(start)

            PressOutcome.LongRelease -> cb.onLongPress(start)

            PressOutcome.Swipe, PressOutcome.Cancel -> Unit

            PressOutcome.LongPress -> {
                cb.onDragStart(start)
                val end = trackDrag(
                    down = down,
                    start = start,
                    first = last?.takeIf { it.pressed },
                    cellAt = { grid().cellAt(it, size) },
                    onMove = cb.onDragMove,
                )
                if (end == start) {
                    cb.onDragCancel()
                    cb.onLongPress(start)
                } else {
                    cb.onDragEnd(start, end)
                }
            }
        }
    }
}

// 드래그 모드: 이벤트를 소비해 월 넘김을 막고 칸이 바뀔 때마다 알림
private suspend fun AwaitPointerEventScope.trackDrag(
    down: PointerInputChange,
    start: LocalDate,
    first: PointerInputChange?,
    cellAt: (Offset) -> LocalDate?,
    onMove: (LocalDate) -> Unit,
): LocalDate {
    var current = start
    // 200ms 판정을 넘긴 첫 이동도 반영
    var pending = first
    while (true) {
        val change = pending ?: awaitPointerEvent().changes.firstOrNull { it.id == down.id }
        pending = null
        if (change == null || !change.pressed) return current
        change.consume()
        val over = cellAt(change.position)
        if (over != null && over != current) {
            current = over
            onMove(over)
        }
    }
}

// 이벤트 시각 기준으로도 200ms 판정 (타이머가 늦어도 같은 결과)
private fun classify(
    change: PointerInputChange?,
    down: PointerInputChange,
    slop: Float,
): PressOutcome? {
    if (change == null) return PressOutcome.Cancel
    val elapsed = change.uptimeMillis - down.uptimeMillis
    val longEnough = elapsed >= LONG_PRESS_MS
    return when {
        !change.pressed -> if (longEnough) PressOutcome.LongRelease else PressOutcome.Tap
        change.isConsumed -> PressOutcome.Cancel
        longEnough -> PressOutcome.LongPress
        (change.position - down.position).getDistance() > slop -> PressOutcome.Swipe
        else -> null
    }
}

private fun MonthGrid.cellAt(position: Offset, size: IntSize): LocalDate? {
    val col = (position.x / (size.width / DAYS_IN_WEEK.toFloat())).toInt()
    val row = (position.y / (size.height / MonthGrid.WEEKS.toFloat())).toInt()
    val inside = position.x >= 0 && position.y >= 0
    return weeks.getOrNull(row)?.getOrNull(col)?.takeIf { inside && it.inMonth }?.date
}

internal class CellActionLabels(
    val mark: String,
    val clear: String,
    val name: String,
    val options: String,
    val from: String,
    val to: String,
)

// TalkBack: 탭 = 지정/해제, 길게 = 시트, 커스텀 액션 = 범위 선택
internal fun Modifier.cellSemantics(
    cell: DayCell,
    description: String,
    labels: CellActionLabels,
    callbacks: GridCallbacks,
): Modifier = clearAndSetSemantics {
    val date = cell.date
    testTag = cellTag(date)
    contentDescription = description
    role = Role.Button
    val tapLabel = when {
        cell.leave != null -> labels.clear
        cell.isHoliday -> labels.name
        else -> labels.mark
    }
    onClick(tapLabel) {
        callbacks.onTap(date)
        true
    }
    onLongClick(labels.options) {
        callbacks.onLongPress(date)
        true
    }
    customActions = listOf(
        CustomAccessibilityAction(labels.from) {
            callbacks.onRangeFrom(date)
            true
        },
        CustomAccessibilityAction(labels.to) {
            callbacks.onRangeTo(date)
            true
        },
    )
}
