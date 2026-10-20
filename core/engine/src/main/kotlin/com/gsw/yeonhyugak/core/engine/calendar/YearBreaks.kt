package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.leave.LeaveLedger
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.LocalDate

// 달력 띠와 같은 기준의 연휴 한 구간
data class BreakSpan(val start: LocalDate, val end: LocalDate, val leaveDays: Int) {
    val days: Int get() = VacationSpan.length(start..end)

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}

// 한 해 연휴 요약. 공유 카드용
data class YearBreaks(val year: Int, val spans: List<BreakSpan>, val usedLeave: Double) {
    val totalDays: Int get() = spans.sumOf { it.days }

    // 카드에 올릴 구간: 내 연차가 든 연휴가 있으면 그것만, 없으면 공휴일 연휴 전체
    fun highlights(): YearBreaks {
        val mine = spans.filter { it.leaveDays > 0 }
        return if (mine.isEmpty()) this else copy(spans = mine)
    }
}

object YearBreakCalculator {
    // 그 해에 걸친 MIN_DAYS 이상 구간. 연도 경계 구간은 양쪽 해에 모두 나온다
    fun of(year: Int, work: WorkCalendar, leaves: Map<LocalDate, LeaveType>): YearBreaks {
        val first = LocalDate.of(year, 1, 1)
        val spans = first.datesUntil(first.plusYears(1))
            .toList()
            .mapNotNull { VacationSpan.around(it, work, leaves) }
            .distinct()
            .filter { VacationSpan.length(it) >= VacationSpan.MIN_DAYS }
            .map { span -> span.toBreak(leaves) }
        return YearBreaks(year, spans, LeaveLedger.used(leaves, year))
    }

    // 내 연차가 하루 이상 들어간 구간만. 연휴 D-21 알림 대상
    fun planned(year: Int, work: WorkCalendar, leaves: Map<LocalDate, LeaveType>): List<BreakSpan> =
        of(year, work, leaves).spans.filter { it.leaveDays > 0 }

    internal fun ClosedRange<LocalDate>.toBreak(leaves: Map<LocalDate, LeaveType>): BreakSpan {
        val inside = start.datesUntil(endInclusive.plusDays(1)).toList()
        return BreakSpan(start, endInclusive, inside.count { leaves[it] == LeaveType.FULL })
    }
}
