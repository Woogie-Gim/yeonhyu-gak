package com.gsw.yeonhyugak.core.engine.planner

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 연휴 블록. leaveDates = 연차로 메운 근무일
data class VacationBlock(
    val start: LocalDate,
    val end: LocalDate,
    val leaveDates: List<LocalDate>,
) {
    val totalDays: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1

    val cost: Int get() = leaveDates.size

    // 연도 경계 블록은 연도별로 따로 차감
    val costByYear: Map<Int, Int> get() = leaveDates.groupingBy { it.year }.eachCount()

    val spansYears: Boolean get() = start.year != end.year

    // 연차 없이 쉬는 구간은 무한대
    val efficiency: Double get() = if (cost ==
        0
    ) {
        Double.POSITIVE_INFINITY
    } else {
        totalDays.toDouble() / cost
    }

    fun overlaps(other: VacationBlock): Boolean =
        !end.isBefore(other.start) && !other.end.isBefore(start)
}
