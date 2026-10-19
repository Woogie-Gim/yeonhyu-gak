package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.SupportedYears
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.LocalDate

// 쉬는 날 = 비근무일 ∨ 연차 1일. 반차는 출근이므로 제외
internal class DayOff(
    private val work: WorkCalendar,
    private val leaves: Map<LocalDate, LeaveType>,
    private val extra: Set<LocalDate> = emptySet(),
) {
    fun isOff(date: LocalDate): Boolean =
        !work.isWorkday(date) || leaves[date] == LeaveType.FULL || date in extra

    // date를 포함한 연속 휴일 구간. 쉬는 날이 아니면 null
    fun runAround(date: LocalDate): ClosedRange<LocalDate>? {
        if (!isOff(date)) return null
        var start = date
        while (inRange(start.minusDays(1)) && isOff(start.minusDays(1))) start = start.minusDays(1)
        var end = date
        while (inRange(end.plusDays(1)) && isOff(end.plusDays(1))) end = end.plusDays(1)
        return start..end
    }

    private fun inRange(date: LocalDate): Boolean = date.year in SupportedYears.range
}
