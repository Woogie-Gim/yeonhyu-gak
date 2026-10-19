package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.SupportedYears
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 연휴 구간 단일 정의. 달력 띠·말풍선·추천 카드가 모두 이 함수를 쓴다
object VacationSpan {
    // 이 일수 이상이어야 연휴 띠·추천 후보
    const val MIN_DAYS = 3

    // date를 포함한 연속 휴일 구간. 쉬는 날이 아니면 null
    // 쉬는 날 = 비근무일 ∨ 연차 1일 ∨ extra(그은 구간·미리보기). 반차는 출근이므로 제외
    fun around(
        date: LocalDate,
        work: WorkCalendar,
        leaves: Map<LocalDate, LeaveType>,
        extra: Set<LocalDate> = emptySet(),
    ): ClosedRange<LocalDate>? {
        val off = { d: LocalDate ->
            !work.isWorkday(d) || leaves[d] == LeaveType.FULL || d in extra
        }
        if (!off(date)) return null
        var start = date
        while (inRange(start.minusDays(1)) && off(start.minusDays(1))) start = start.minusDays(1)
        var end = date
        while (inRange(end.plusDays(1)) && off(end.plusDays(1))) end = end.plusDays(1)
        return start..end
    }

    // 미리보기 연차가 만드는 연휴 구간들. 날짜순
    fun spansOf(
        dates: Set<LocalDate>,
        work: WorkCalendar,
        leaves: Map<LocalDate, LeaveType>,
    ): List<ClosedRange<LocalDate>> =
        dates.mapNotNull { around(it, work, leaves, dates) }.distinct().sortedBy { it.start }

    fun length(span: ClosedRange<LocalDate>): Int =
        ChronoUnit.DAYS.between(span.start, span.endInclusive).toInt() + 1

    private fun inRange(date: LocalDate): Boolean = date.year in SupportedYears.range
}
