package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.SupportedYears
import com.gsw.yeonhyugak.core.engine.calendar.YearBreakCalculator.toBreak
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 위젯 D-n. daysLeft 0 = 오늘이 연휴 안
data class NextBreak(val span: BreakSpan, val daysLeft: Int) {
    val ongoing: Boolean get() = daysLeft == 0
}

object NextBreakFinder {
    // 오늘 포함 이후 첫 연휴. 범위 끝까지 없으면 null
    fun from(today: LocalDate, work: WorkCalendar, leaves: Map<LocalDate, LeaveType>): NextBreak? {
        var d = today
        val last = LocalDate.of(SupportedYears.LAST, 12, 31)
        while (!d.isAfter(last)) {
            val span = VacationSpan.around(d, work, leaves)
            if (span == null) {
                d = d.plusDays(1)
                continue
            }
            if (VacationSpan.length(span) >= VacationSpan.MIN_DAYS) {
                val left = maxOf(0, ChronoUnit.DAYS.between(today, span.start).toInt())
                return NextBreak(span.toBreak(leaves), left)
            }
            d = span.endInclusive.plusDays(1)
        }
        return null
    }
}
