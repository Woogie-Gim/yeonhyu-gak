package com.gsw.yeonhyugak.core.engine.planner

import java.time.LocalDate
import java.time.Month

enum class Season(val months: Set<Month>) {
    SPRING(setOf(Month.MARCH, Month.APRIL, Month.MAY)),
    SUMMER(setOf(Month.JUNE, Month.JULY, Month.AUGUST)),
    AUTUMN(setOf(Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER)),
    WINTER(setOf(Month.DECEMBER, Month.JANUARY, Month.FEBRUARY)),
}

enum class SortOrder { EFFICIENCY, LENGTH, DATE }

// 필터 칩. 기준은 블록 시작일
data class CandidateFilter(
    val from: LocalDate? = null,
    val season: Season? = null,
    val month: Month? = null,
    val minLength: Int? = null,
) {
    fun accepts(block: VacationBlock): Boolean = (from == null || !block.start.isBefore(from)) &&
        (season == null || block.start.month in season.months) &&
        (month == null || block.start.month == month) &&
        (minLength == null || block.totalDays >= minLength)

    companion object {
        val NONE = CandidateFilter()

        // 칩 값: 3일+ · 5일+ · 9일+
        val MIN_LENGTH_CHIPS = listOf(3, 5, 9)
    }
}
