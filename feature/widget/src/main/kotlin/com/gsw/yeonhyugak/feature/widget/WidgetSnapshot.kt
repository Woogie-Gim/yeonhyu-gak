package com.gsw.yeonhyugak.feature.widget

import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.SeoulTime
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import com.gsw.yeonhyugak.core.engine.calendar.MonthGrid
import com.gsw.yeonhyugak.core.engine.calendar.NextBreak
import com.gsw.yeonhyugak.core.engine.calendar.NextBreakFinder
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.leave.LeaveLedger
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// 위젯 한 번 그릴 때 필요한 값. 계산은 엔진, 여기서는 모으기만
data class WidgetSnapshot(
    val today: LocalDate,
    val appearance: Appearance,
    val next: NextBreak?,
    // 연휴 이름에 쓸 공휴일. 없으면 `n일 연휴`
    val nextHoliday: Holiday?,
    val remaining: Double,
    val grid: MonthGrid,
)

class WidgetSnapshotLoader @Inject constructor(
    private val holidays: HolidayRepository,
    private val leaves: LeaveRepository,
    private val prefs: UserPrefsRepository,
    private val clock: Clock,
) {
    suspend fun load(): WidgetSnapshot {
        val today = SeoulTime.today(clock)
        val calendar = holidays.calendar()
        val work = WorkCalendar(holidays = calendar.holidayDates)
        val marked = leaves.observe().first()
        val next = NextBreakFinder.from(today, work, marked)
        val named = next?.let { n ->
            calendar.within(n.span.start..n.span.end)
                .sortedBy { it.kind == HolidayKind.SUBSTITUTE }
                .firstOrNull()
        }
        return WidgetSnapshot(
            today = today,
            appearance = prefs.appearance.first(),
            next = next,
            nextHoliday = named,
            remaining = LeaveLedger.remaining(prefs.allowance.first(), marked, today.year),
            grid = MonthGrid.build(YearMonth.from(today), calendar, work, marked),
        )
    }
}
