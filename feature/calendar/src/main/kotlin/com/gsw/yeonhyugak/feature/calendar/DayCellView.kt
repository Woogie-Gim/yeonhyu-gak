package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gsw.yeonhyugak.core.engine.calendar.BlockBand
import com.gsw.yeonhyugak.core.engine.calendar.DayCell
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import java.time.DayOfWeek

private val CellShape = RoundedCornerShape(8.dp)
private val TopHalfShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
private val BottomHalfShape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)

// 색 + 모양 이중 표기 (SPEC 6장). 메모는 점 하나, 내용은 그리지 않는다
@Composable
internal fun DayCellView(
    cell: DayCell,
    isToday: Boolean,
    hasMemo: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = YhTheme.colors
    // 띠 영역은 글자 크기를 따라 커진다 (200%에서 라벨 잘림 방지)
    val bandArea = with(LocalDensity.current) { BAND_AREA.toDp() }
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .padding(bottom = bandArea),
        ) {
            LeaveFill(cell.leave)
            if (cell.isExpectedHoliday) ExpectedBorder(colors.holiday)
            if (selected) {
                Box(Modifier.fillMaxSize().border(2.dp, colors.accent, CellShape))
            }
            Column(
                Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DayNumber(cell, isToday, Modifier.weight(1f))
                cell.holidays.firstOrNull()?.let {
                    AutoText(
                        text = holidayShortName(it),
                        color = if (cell.leave ==
                            LeaveType.FULL
                        ) {
                            colors.onAccent
                        } else {
                            colors.holiday
                        },
                        maxSize = 10.sp,
                        modifier = Modifier.weight(HOLIDAY_LABEL_WEIGHT).fillMaxWidth(),
                    )
                }
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
        cell.band?.let { Band(it, bandArea, Modifier.align(Alignment.BottomCenter)) }
    }
}

@Composable
private fun LeaveFill(leave: LeaveType?) {
    val accent = YhTheme.colors.accent
    // 반차는 숫자가 겹치므로 옅게 채워 글자 대비 유지
    val half = accent.copy(alpha = HALF_FILL_ALPHA)
    when (leave) {
        LeaveType.FULL -> Box(Modifier.fillMaxSize().background(accent, CellShape))

        // 오전 = 위 반쪽
        LeaveType.AM_HALF -> Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().fillMaxHeight(HALF).background(half, TopHalfShape))
        }

        // 오후 = 아래 반쪽
        LeaveType.PM_HALF -> Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(HALF)
                    .background(half, BottomHalfShape),
            )
        }

        null -> Unit
    }
}

// 예상 공휴일: 점선 테두리
@Composable
private fun ExpectedBorder(color: Color) {
    Box(
        Modifier.fillMaxSize().drawBehind {
            val stroke = 1.5.dp.toPx()
            val dash = 4.dp.toPx()
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(8.dp.toPx()),
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
        cell.leave == LeaveType.FULL -> colors.onAccent
        cell.isHoliday || cell.dayOfWeek == DayOfWeek.SUNDAY -> colors.holiday
        cell.dayOfWeek == DayOfWeek.SATURDAY -> colors.saturday
        else -> colors.ink
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val ring = if (isToday) {
            Modifier.border(
                1.5.dp,
                if (cell.leave ==
                    LeaveType.FULL
                ) {
                    colors.onAccent
                } else {
                    colors.ink
                },
                CircleShape,
            )
        } else {
            Modifier
        }
        Box(ring.padding(horizontal = 6.dp, vertical = 1.dp), contentAlignment = Alignment.Center) {
            AutoText(
                text = cell.date.dayOfMonth.toString(),
                color = color,
                maxSize = 16.sp,
                weight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

// 연휴 띠: 블록 전체를 잇는 얇은 띠 + 시작에 `n일`
@Composable
private fun Band(band: BlockBand, area: Dp, modifier: Modifier) {
    val colors = YhTheme.colors
    Box(modifier.fillMaxWidth().height(area)) {
        val shape = RoundedCornerShape(
            topStart = if (band.isStart) 2.dp else 0.dp,
            bottomStart = if (band.isStart) 2.dp else 0.dp,
            topEnd = if (band.isEnd) 2.dp else 0.dp,
            bottomEnd = if (band.isEnd) 2.dp else 0.dp,
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = if (band.isStart) 4.dp else 0.dp,
                    end = if (band.isEnd) 4.dp else 0.dp,
                )
                .fillMaxWidth()
                .height(3.dp)
                .clip(shape)
                .background(colors.blockBand),
        )
        if (band.labelHere) {
            AutoText(
                text = stringResource(R.string.cal_band_days, band.length),
                color = colors.ink,
                maxSize = 9.sp,
                align = TextAlign.Start,
                modifier = Modifier.align(
                    Alignment.TopStart,
                ).padding(start = 4.dp).height(area - 3.dp),
            )
        }
    }
}

// 글자 크기 200%에서도 칸 안에 맞게 숫자만 줄인다
@Composable
private fun AutoText(
    text: String,
    color: Color,
    maxSize: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Center,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(fontWeight = weight, textAlign = align),
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

private val BAND_AREA = 13.sp
private val MIN_TEXT = 6.sp
private const val HOLIDAY_LABEL_WEIGHT = 0.6f
private const val HALF = 0.5f
private const val HALF_FILL_ALPHA = 0.45f
