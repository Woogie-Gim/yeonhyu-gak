package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.leave.LeaveLedger
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.LocalDate

// 그은 구간 결과. 말풍선 `연차 3일 → 9일 휴가` · `2일 초과`
data class RangeSummary(
    val start: LocalDate,
    val end: LocalDate,
    // 새로 연차 1일이 되는 날 (근무일만)
    val leaveDates: List<LocalDate>,
    val cost: Double,
    val totalDays: Int,
    val overBy: Double,
) {
    val isOver: Boolean get() = overBy > 0.0
}

object RangeSelection {
    // allowance = 연도별 쓸 수 있는 연차 총량. 초과해도 막지 않는다
    fun summarize(
        from: LocalDate,
        to: LocalDate,
        work: WorkCalendar,
        leaves: Map<LocalDate, LeaveType>,
        allowance: Double,
    ): RangeSummary {
        val start = minOf(from, to)
        val end = maxOf(from, to)
        // 주말·공휴일·회사 휴무일·이미 연차인 날 제외 후 차감
        val booked = leaves.filterValues { it == LeaveType.FULL }.keys
        val leaveDates = work.copy(bookedLeave = work.bookedLeave + booked).leaveDaysIn(start, end)
        val costOf = { d: LocalDate -> 1.0 - (leaves[d]?.amount ?: 0.0) }
        val cost = leaveDates.sumOf(costOf)

        val drawn = start.datesUntil(end.plusDays(1)).toList().toSet()
        val run = VacationSpan.around(start, work, leaves, extra = drawn) ?: (start..end)
        val totalDays = VacationSpan.length(run)

        // 연도별로 따로 초과 계산
        val used = LeaveLedger.usedByYear(leaves)
        val overBy = leaveDates.groupBy { it.year }.entries.sumOf { (year, dates) ->
            val after = (used[year] ?: 0.0) + dates.sumOf(costOf)
            (after - allowance).coerceAtLeast(0.0).coerceAtMost(dates.sumOf(costOf))
        }
        return RangeSummary(start, end, leaveDates, cost, totalDays, overBy)
    }
}
