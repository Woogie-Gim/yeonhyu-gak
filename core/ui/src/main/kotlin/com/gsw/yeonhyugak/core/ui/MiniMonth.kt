package com.gsw.yeonhyugak.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gsw.yeonhyugak.core.engine.calendar.DayCell
import com.gsw.yeonhyugak.core.engine.calendar.MonthGrid
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import java.time.DayOfWeek
import java.time.LocalDate

// 테마 미리보기용 축소 달력. 실제 내 연차·공휴일로 그린다. 조작 없음
@Composable
fun MiniMonth(
    grid: MonthGrid,
    today: LocalDate,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = YhTheme.colors
    Box(
        modifier
            .background(colors.background)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        MonthBackdrop(grid.month.monthValue, Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(4.dp)) {
            grid.weeks.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        Box(Modifier.weight(1f).aspectRatio(CELL_RATIO).padding(1.dp)) {
                            if (cell.inMonth) MiniCell(cell, cell.date == today)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCell(cell: DayCell, isToday: Boolean) {
    val colors = YhTheme.colors
    val shape = RoundedCornerShape(YhTheme.spec.cellStyle.corner / 2)
    val fill = when (cell.leave) {
        LeaveType.FULL -> Modifier.background(colors.leaveFill, shape)
        LeaveType.AM_HALF, LeaveType.PM_HALF -> Modifier.background(colors.halfLeaveFill, shape)
        null -> Modifier
    }
    val color = when {
        cell.leave == LeaveType.FULL -> colors.onLeaveFill
        cell.isHoliday || cell.dayOfWeek == DayOfWeek.SUNDAY -> colors.holiday
        cell.dayOfWeek == DayOfWeek.SATURDAY -> colors.saturday
        else -> colors.ink
    }
    val ring = if (isToday) {
        val onLeave = cell.leave == LeaveType.FULL
        val ringColor = if (onLeave) colors.todayRingOnLeave else colors.todayRing
        Modifier.border(1.dp, ringColor, CircleShape)
    } else {
        Modifier
    }
    Box(Modifier.fillMaxSize().then(fill), contentAlignment = Alignment.Center) {
        BasicText(
            text = cell.date.dayOfMonth.toString(),
            modifier = ring.padding(horizontal = 2.dp),
            style = TextStyle(fontFamily = YhTheme.fonts.numeral, textAlign = TextAlign.Center),
            color = { color },
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 6.sp,
                maxFontSize = 11.sp,
                stepSize = 0.5.sp,
            ),
        )
    }
}

private const val CELL_RATIO = 1.3f
