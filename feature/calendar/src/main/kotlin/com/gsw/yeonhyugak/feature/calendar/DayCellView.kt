package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gsw.yeonhyugak.core.engine.calendar.DayCell
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import java.time.DayOfWeek
import java.time.LocalDate

// 칸 단위 테스트용. 실제 화면은 cellSemantics가 덮어쓴다
internal fun dayNumberTag(date: LocalDate): String = "cal_num_$date"

// 칸 모서리는 테마 cellStyle
@Composable
private fun cellShape(): Shape = RoundedCornerShape(YhTheme.spec.cellStyle.corner)

// 색 + 모양 이중 표기 (SPEC 6장). 메모는 점 하나, 내용은 그리지 않는다
// 원격으로 새로 추가된 공휴일은 왼쪽 위 `N` 글자 (메모 점과 다른 모양)
@Composable
internal fun DayCellView(
    cell: DayCell,
    isToday: Boolean,
    hasMemo: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
) {
    val colors = YhTheme.colors
    val shape = cellShape()
    // 최대 높이는 글자 크기를 따라 커진다. 줄이 낮으면 비율대로 줄고 글자도 줄어든다
    val density = LocalDensity.current
    val topMax = with(density) { NUMBER_AREA.toDp() + NAME_AREA.toDp() } + 4.dp
    val labelMax = with(density) { LABEL_AREA.toDp() }
    // 위에서부터 쌓는다. 남는 공간은 아래로 가서 다음 주와의 간격이 된다
    // 띠는 옆 칸과 이어져야 하므로 좌우 여백은 숫자 칸에만
    Column(modifier.fillMaxSize().padding(vertical = 2.dp)) {
        Box(
            Modifier
                .weight(1f)
                .heightIn(max = topMax)
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
        ) {
            LeaveFill(cell.leave, shape)
            if (cell.isPreview) PreviewFill(colors.accent, shape)
            if (cell.isExpectedHoliday) ExpectedBorder(colors.holiday)
            if (selected) {
                Box(Modifier.fillMaxSize().border(2.dp, colors.accent, shape))
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DayNumber(cell, isToday, Modifier.weight(1f))
                // 이름 영역은 항상 확보. 모든 칸의 숫자 위치를 맞춘다
                Box(Modifier.weight(HOLIDAY_LABEL_WEIGHT).fillMaxWidth()) { HolidayLabel(cell) }
            }
            if (isNew) {
                NewBadge(
                    onLeave = cell.leave == LeaveType.FULL,
                    Modifier.align(Alignment.TopStart),
                )
            }
            if (hasMemo) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(5.dp)
                        .background(colors.memoDot, CircleShape),
                )
            }
        }
        // 띠·라벨 자리는 띠가 없어도 확보 (칸마다 숫자 위치 동일)
        Box(Modifier.fillMaxWidth().height(ARROW_HEIGHT)) {
            cell.band?.let { BandRow(it, cell.date) }
        }
        Box(Modifier.weight(LABEL_WEIGHT).heightIn(max = labelMax).fillMaxWidth()) {
            cell.band?.takeIf { it.labelHere }?.let { BandLabel(it) }
        }
        Spacer(Modifier.weight(GAP_WEIGHT))
    }
}

@Composable
private fun NewBadge(onLeave: Boolean, modifier: Modifier) {
    val colors = YhTheme.colors
    BasicText(
        text = "N",
        style = TextStyle(
            color = if (onLeave) colors.onLeaveFill else colors.holiday,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = modifier.padding(start = 3.dp, top = 1.dp).testTag(NEW_BADGE_TAG),
    )
}

internal const val NEW_BADGE_TAG = "cal_new_badge"

// 숫자 아래 공휴일 이름 축약 (색 + 글자)
@Composable
private fun HolidayLabel(cell: DayCell) {
    val holiday = cell.holidays.firstOrNull() ?: return
    val colors = YhTheme.colors
    AutoText(
        text = AnnotatedString(holidayShortName(holiday)),
        color = if (cell.leave == LeaveType.FULL) colors.onLeaveFill else colors.holiday,
        maxSize = 10.sp,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun LeaveFill(leave: LeaveType?, shape: Shape) {
    val colors = YhTheme.colors
    val corner = YhTheme.spec.cellStyle.corner
    when (leave) {
        LeaveType.FULL -> Box(Modifier.fillMaxSize().background(colors.leaveFill, shape))

        // 오전 = 위 반쪽. 숫자가 겹치므로 옅게 채우고 바깥 변에 진한 줄 (그래픽 3:1)
        LeaveType.AM_HALF -> Box(Modifier.fillMaxSize()) {
            HalfFill(RoundedCornerShape(topStart = corner, topEnd = corner), top = true)
        }

        // 오후 = 아래 반쪽
        LeaveType.PM_HALF -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            HalfFill(RoundedCornerShape(bottomStart = corner, bottomEnd = corner), top = false)
        }

        null -> Unit
    }
}

@Composable
private fun HalfFill(shape: Shape, top: Boolean) {
    val colors = YhTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(HALF)
            .clip(shape)
            .background(colors.halfLeaveFill)
            .drawBehind {
                val edge = HALF_EDGE.toPx()
                val y = if (top) 0f else size.height - edge
                drawRect(colors.leaveFill, Offset(0f, y), Size(size.width, edge))
            },
    )
}

// 담기 전 미리보기: 옅은 채움 + 빗금 + 실선 테두리 (색만으로 구분하지 않음)
@Composable
private fun PreviewFill(color: Color, shape: Shape) {
    Box(
        Modifier
            .fillMaxSize()
            .background(color.copy(alpha = PREVIEW_FILL_ALPHA), shape)
            .border(1.5.dp, color, shape)
            // 빗금이 옆 칸으로 넘치지 않게
            .clip(shape)
            .drawBehind {
                val gap = 6.dp.toPx()
                val stroke = 1.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = color.copy(alpha = PREVIEW_HATCH_ALPHA),
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = stroke,
                    )
                    x += gap
                }
            },
    )
}

// 예상 공휴일: 점선 테두리
@Composable
private fun ExpectedBorder(color: Color) {
    val corner = YhTheme.spec.cellStyle.corner
    Box(
        Modifier.fillMaxSize().drawBehind {
            val stroke = 1.5.dp.toPx()
            val dash = 4.dp.toPx()
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(corner.toPx()),
                style = Stroke(
                    width = stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                ),
            )
        },
    )
}

@Composable
private fun DayNumber(cell: DayCell, isToday: Boolean, modifier: Modifier) {
    val colors = YhTheme.colors
    val color = when {
        cell.leave == LeaveType.FULL -> colors.onLeaveFill
        cell.isHoliday || cell.dayOfWeek == DayOfWeek.SUNDAY -> colors.holiday
        cell.dayOfWeek == DayOfWeek.SATURDAY -> colors.saturday
        else -> colors.ink
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // 오늘: 링 + 굵은 숫자 (색만으로 구분하지 않음)
        val ring = if (isToday) {
            val onLeave = cell.leave == LeaveType.FULL
            val ringColor = if (onLeave) colors.todayRingOnLeave else colors.todayRing
            Modifier.border(1.5.dp, ringColor, CircleShape)
        } else {
            Modifier
        }
        Box(ring.padding(horizontal = 6.dp, vertical = 1.dp), contentAlignment = Alignment.Center) {
            AutoText(
                text = AnnotatedString(cell.date.dayOfMonth.toString()),
                color = color,
                maxSize = 16.sp,
                weight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                family = YhTheme.fonts.numeral,
                modifier = Modifier.testTag(dayNumberTag(cell.date)),
            )
        }
    }
}

// 글자 크기 200%에서도 칸 안에 맞게 숫자만 줄인다
@Composable
internal fun AutoText(
    text: AnnotatedString,
    color: Color,
    maxSize: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Center,
    family: FontFamily = YhTheme.fonts.body,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(fontFamily = family, fontWeight = weight, textAlign = align),
        color = { color },
        maxLines = 1,
        // 줄바꿈 허용 + 1줄 제한이어야 넘침을 감지해 글자를 줄인다
        softWrap = true,
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(
            minFontSize = MIN_TEXT,
            maxFontSize = maxSize,
            stepSize = 0.5.sp,
        ),
    )
}

private val NUMBER_AREA = 24.sp
private val NAME_AREA = 14.sp
private val LABEL_AREA = 16.sp
private val ARROW_HEIGHT = 8.dp
private val MIN_TEXT = 6.sp
private const val HOLIDAY_LABEL_WEIGHT = 0.6f
private const val LABEL_WEIGHT = 0.5f
private const val GAP_WEIGHT = 0.25f
private const val HALF = 0.5f
private val HALF_EDGE = 2.dp
private const val PREVIEW_FILL_ALPHA = 0.18f
private const val PREVIEW_HATCH_ALPHA = 0.5f
