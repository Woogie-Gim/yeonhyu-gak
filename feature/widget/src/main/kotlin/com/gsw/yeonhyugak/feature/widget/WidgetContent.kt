package com.gsw.yeonhyugak.feature.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.gsw.yeonhyugak.core.data.holiday.HolidayLabels
import com.gsw.yeonhyugak.core.data.nav.OpenDateStore
import com.gsw.yeonhyugak.core.engine.calendar.DayCell
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.DayCount
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

// 탭 → 그 날짜로 앱 열기. data URI로 날짜마다 다른 PendingIntent
internal fun openDate(context: Context, date: LocalDate): Action {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    intent.data = Uri.parse("yeonhyugak://open/${date.toEpochDay()}")
    intent.putExtra(OpenDateStore.EXTRA_OPEN_DATE, date.toEpochDay())
    return actionStartActivity(intent)
}

// 2×1: 다음 연휴까지 D-n + 연휴 이름
@Composable
fun NextBreakContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val palette = WidgetPalette.of(snapshot.appearance, snapshot.today.monthValue)
    val next = snapshot.next
    val open = next?.span?.start ?: snapshot.today
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(openDate(context, open)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            next == null -> Text(
                context.getString(R.string.widget_next_none),
                style = TextStyle(color = palette.ink, fontSize = 14.sp),
            )

            next.ongoing -> {
                Text(
                    context.getString(R.string.widget_next_ongoing),
                    style = TextStyle(
                        color = palette.accent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                BreakName(snapshot, palette)
            }

            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        context.getString(R.string.widget_next_title),
                        style = TextStyle(color = palette.muted, fontSize = 12.sp),
                    )
                    Spacer(GlanceModifier.padding(start = 6.dp))
                    Text(
                        context.getString(R.string.widget_next_dday, next.daysLeft),
                        style = TextStyle(
                            color = palette.accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                BreakName(snapshot, palette)
            }
        }
    }
}

@Composable
private fun BreakName(snapshot: WidgetSnapshot, palette: WidgetPalette) {
    val context = LocalContext.current
    val next = snapshot.next ?: return
    val holiday = snapshot.nextHoliday
    val text = if (holiday == null) {
        context.getString(R.string.widget_next_unnamed, next.span.days)
    } else {
        val name = HolidayLabels.displayName(holiday) { id, args -> context.getString(id, *args) }
        context.getString(R.string.widget_next_named, name, next.span.days)
    }
    Text(text, style = TextStyle(color = palette.ink, fontSize = 13.sp), maxLines = 1)
}

// 4×2: 이번 달 미니 달력 + 남은 연차
@Composable
fun MonthContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val palette = WidgetPalette.of(snapshot.appearance, snapshot.today.monthValue)
    val weeks = snapshot.grid.weeks.filter { week -> week.any { it.inMonth } }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(openDate(context, snapshot.today)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                context.getString(R.string.widget_month_title, snapshot.today.monthValue),
                style = TextStyle(
                    color = palette.ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                context.getString(R.string.widget_remaining, DayCount.format(snapshot.remaining)),
                style = TextStyle(
                    color = palette.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            WEEK.forEach { day ->
                Text(
                    day.getDisplayName(DateTextStyle.NARROW, Locale.KOREAN),
                    style = TextStyle(
                        color = weekdayColor(day, palette),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        weeks.forEach { week ->
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                week.forEach { cell ->
                    Box(modifier = GlanceModifier.defaultWeight().fillMaxSize()) {
                        if (cell.inMonth) DayBox(cell, snapshot.today, palette)
                    }
                }
            }
        }
    }
}

// 공휴일 = 색 + 밑줄, 연차 = 칸 채움, 반차 = 위/아래 반쪽 채움 (SPEC 6장)
@Composable
private fun DayBox(cell: DayCell, today: LocalDate, palette: WidgetPalette) {
    val context = LocalContext.current
    val full = cell.leave == LeaveType.FULL
    val color = when {
        full -> palette.onLeaveFill
        cell.isHoliday || cell.dayOfWeek == DayOfWeek.SUNDAY -> palette.holiday
        cell.dayOfWeek == DayOfWeek.SATURDAY -> palette.saturday
        else -> palette.ink
    }
    val description = buildString {
        append(cell.date.monthValue).append('/').append(cell.date.dayOfMonth)
        cell.holidays.firstOrNull()?.let {
            append(' ').append(
                HolidayLabels.displayName(it) { id, args ->
                    context.getString(id, *args)
                },
            )
        }
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openDate(context, cell.date))
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        LeaveFill(cell.leave, palette)
        Text(
            cell.date.dayOfMonth.toString(),
            style = TextStyle(
                color = color,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                fontWeight = if (cell.date == today) FontWeight.Bold else FontWeight.Normal,
                textDecoration = if (cell.isHoliday) TextDecoration.Underline else null,
            ),
        )
    }
}

@Composable
private fun LeaveFill(leave: LeaveType?, palette: WidgetPalette) {
    val top: ColorProvider? = when (leave) {
        LeaveType.FULL -> palette.leaveFill
        LeaveType.AM_HALF -> palette.halfLeaveFill
        else -> null
    }
    val bottom: ColorProvider? = when (leave) {
        LeaveType.FULL -> palette.leaveFill
        LeaveType.PM_HALF -> palette.halfLeaveFill
        else -> null
    }
    if (top == null && bottom == null) return
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Half(top)
        Half(bottom)
    }
}

@Composable
private fun ColumnScope.Half(fill: ColorProvider?) {
    val base = GlanceModifier.fillMaxWidth().defaultWeight()
    Spacer(if (fill == null) base else base.background(fill))
}

private fun weekdayColor(day: DayOfWeek, palette: WidgetPalette): ColorProvider = when (day) {
    DayOfWeek.SUNDAY -> palette.holiday
    DayOfWeek.SATURDAY -> palette.saturday
    else -> palette.muted
}

private val WEEK = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }
