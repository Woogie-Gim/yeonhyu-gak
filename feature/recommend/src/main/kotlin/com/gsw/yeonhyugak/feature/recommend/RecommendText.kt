package com.gsw.yeonhyugak.feature.recommend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.gsw.yeonhyugak.core.data.holiday.HolidayLabels
import com.gsw.yeonhyugak.core.engine.holiday.HolidayCalendar
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.planner.Recommendation
import com.gsw.yeonhyugak.core.ui.DayCount
import java.time.LocalDate
import kotlin.math.roundToInt

private const val TENTHS = 10.0

// 5월 2일(금). 연도를 넘는 블록은 연도까지
@Composable
internal fun cardDate(date: LocalDate, withYear: Boolean): String {
    val weekday = stringArrayResource(R.array.rec_weekday_short)[date.dayOfWeek.value % 7]
    return if (withYear) {
        stringResource(
            R.string.rec_card_date_year,
            date.year,
            date.monthValue,
            date.dayOfMonth,
            weekday,
        )
    } else {
        stringResource(R.string.rec_card_date, date.monthValue, date.dayOfMonth, weekday)
    }
}

@Composable
internal fun cardRange(item: Recommendation): String = stringResource(
    R.string.rec_card_range,
    cardDate(item.start, item.spansYears),
    cardDate(item.end, item.spansYears),
)

// 효율 2.5배. 소수 첫째 자리까지
internal fun efficiencyText(efficiency: Double): String =
    DayCount.format((efficiency * TENTHS).roundToInt() / TENTHS)

// 구간 안 대표 공휴일 표시 이름. 대체공휴일보다 원래 공휴일 우선
@Composable
@ReadOnlyComposable
internal fun holidayLabel(holidays: HolidayCalendar?, item: Recommendation): String? {
    val list = holidays?.within(item.span).orEmpty()
    val main = list.firstOrNull { it.kind != HolidayKind.SUBSTITUTE } ?: list.firstOrNull()
    val res = LocalResources.current
    return main?.let { HolidayLabels.displayName(it) { id, args -> res.getString(id, *args) } }
}
