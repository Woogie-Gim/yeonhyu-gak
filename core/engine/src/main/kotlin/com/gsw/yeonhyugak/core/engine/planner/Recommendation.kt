package com.gsw.yeonhyugak.core.engine.planner

import com.gsw.yeonhyugak.core.engine.calendar.VacationSpan
import java.time.LocalDate

enum class RecommendPeriod { REST_OF_YEAR, NEXT_YEAR }

// 추천 카드 한 장. span은 VacationSpan 결과라 달력 띠와 일수가 같다
// bookedInside = 구간 안에 이미 지정돼 있던 연차 1일 수
data class Recommendation(
    val leaveDates: List<LocalDate>,
    val span: ClosedRange<LocalDate>,
    val bookedInside: Int = 0,
) : Vacation {
    override val start: LocalDate get() = span.start

    val end: LocalDate get() = span.endInclusive

    override val totalDays: Int get() = VacationSpan.length(span)

    val cost: Int get() = leaveDates.size

    val costByYear: Map<Int, Int> get() = leaveDates.groupingBy { it.year }.eachCount()

    val spansYears: Boolean get() = start.year != end.year

    override val efficiency: Double get() = efficiencyOf(totalDays, cost)

    fun overlaps(other: Recommendation): Boolean =
        !end.isBefore(other.start) && !other.end.isBefore(start)
}
