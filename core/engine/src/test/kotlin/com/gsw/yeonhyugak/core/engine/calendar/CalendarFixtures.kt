package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayBundle
import com.gsw.yeonhyugak.core.engine.holiday.HolidayCalendar
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.holiday.HolidayOverrides
import com.gsw.yeonhyugak.core.engine.holiday.HolidaySource
import com.gsw.yeonhyugak.core.engine.holiday.KnownLunarDates
import com.gsw.yeonhyugak.core.engine.holiday.LunarTable
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.LocalDate

// 2026 확정(번들) + 원격 선거일, 2024 예상
object CalendarFixtures {
    fun d(m: Int, day: Int, y: Int = 2026): LocalDate = LocalDate.of(y, m, day)

    val labor = Holiday(d(5, 1), "노동절", HolidayKind.NATIONAL, true, HolidaySource.BUNDLED)
    val children = Holiday(d(5, 5), "어린이날", HolidayKind.NATIONAL, true, HolidaySource.BUNDLED)
    val election =
        Holiday(d(6, 3), "전국동시지방선거", HolidayKind.ELECTION, true, HolidaySource.REMOTE)

    val holidays = HolidayCalendar(
        bundle = HolidayBundle(mapOf(2026 to listOf(labor, children))),
        lunar = LunarTable(mapOf(2024 to KnownLunarDates.Y2024, 2025 to KnownLunarDates.Y2025)),
        overrides = HolidayOverrides(add = listOf(election)),
        years = 2024..2026,
    )

    val work = WorkCalendar(holidays = holidays.holidayDates)
}
