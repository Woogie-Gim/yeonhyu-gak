package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.gsw.yeonhyugak.core.data.holiday.HolidayLabels
import com.gsw.yeonhyugak.core.engine.calendar.DayCell
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.time.DayOfWeek
import java.time.LocalDate

// 일요일 = 0
internal val DayOfWeek.sundayIndex: Int get() = value % 7

@Composable
@ReadOnlyComposable
internal fun holidayName(holiday: Holiday): String {
    val res = LocalResources.current
    return HolidayLabels.displayName(holiday) { id, args -> res.getString(id, *args) }
}

// 칸 아래 이름 축약. 긴 선거·임시공휴일은 종류로 줄이고 전체 이름은 툴팁에
@Composable
@ReadOnlyComposable
internal fun holidayShortName(holiday: Holiday): String = when (holiday.kind) {
    HolidayKind.SUBSTITUTE -> stringResource(R.string.cal_holiday_short_substitute)
    HolidayKind.ELECTION -> stringResource(R.string.cal_holiday_short_election)
    HolidayKind.TEMPORARY -> stringResource(R.string.cal_holiday_short_temporary)
    HolidayKind.NATIONAL, HolidayKind.LUNAR -> holidayName(holiday)
}

@Composable
internal fun dateLabel(date: LocalDate): String = stringResource(
    R.string.cal_date_full,
    date.monthValue,
    date.dayOfMonth,
    stringArrayResource(R.array.cal_weekday_full)[date.dayOfWeek.sundayIndex],
)

// TalkBack: "5월 2일 금요일 근무일" 형태
@Composable
internal fun cellDescription(
    cell: DayCell,
    isToday: Boolean,
    hasMemo: Boolean,
    selected: Boolean,
): String {
    val parts = mutableListOf(dateLabel(cell.date))
    if (isToday) parts += stringResource(R.string.cal_a11y_today)
    val names = cell.holidays.map { holidayName(it) }.joinToString()
    parts += when {
        cell.isExpectedHoliday -> stringResource(R.string.cal_a11y_expected_holiday, names)
        cell.isHoliday -> stringResource(R.string.cal_a11y_holiday, names)
        cell.isWorkday -> stringResource(R.string.cal_a11y_workday)
        else -> stringResource(R.string.cal_a11y_weekend)
    }
    when (cell.leave) {
        LeaveType.FULL -> parts += stringResource(R.string.cal_a11y_full)
        LeaveType.AM_HALF -> parts += stringResource(R.string.cal_a11y_am)
        LeaveType.PM_HALF -> parts += stringResource(R.string.cal_a11y_pm)
        null -> Unit
    }
    if (cell.isPreview) parts += stringResource(R.string.cal_a11y_preview)
    cell.band?.let { parts += stringResource(R.string.cal_a11y_band, it.length) }
    if (hasMemo) parts += stringResource(R.string.cal_a11y_memo)
    if (selected) parts += stringResource(R.string.cal_a11y_selected)
    return parts.joinToString(" · ")
}
