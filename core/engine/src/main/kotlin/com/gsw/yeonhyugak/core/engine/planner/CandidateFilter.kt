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

// 정렬·필터 공통 속성. VacationBlock · Recommendation
interface Vacation {
    val start: LocalDate
    val totalDays: Int
    val efficiency: Double
}

// 기본 = 효율 높은 순 → 같으면 긴 순
fun <T : Vacation> SortOrder.comparator(): Comparator<T> = when (this) {
    SortOrder.EFFICIENCY -> compareByDescending<T> { it.efficiency }
        .thenByDescending { it.totalDays }
        .thenBy { it.start }

    SortOrder.LENGTH -> compareByDescending<T> { it.totalDays }
        .thenByDescending { it.efficiency }
        .thenBy { it.start }

    SortOrder.DATE -> compareBy<T> { it.start }.thenByDescending { it.totalDays }
}

// 필터 칩. 기준은 블록 시작일
data class CandidateFilter(
    val from: LocalDate? = null,
    val season: Season? = null,
    val month: Month? = null,
    val minLength: Int? = null,
) {
    fun accepts(v: Vacation): Boolean = (from == null || !v.start.isBefore(from)) &&
        (season == null || v.start.month in season.months) &&
        (month == null || v.start.month == month) &&
        (minLength == null || v.totalDays >= minLength)

    companion object {
        val NONE = CandidateFilter()

        // 칩 값: 3일+ · 5일+ · 9일+
        val MIN_LENGTH_CHIPS = listOf(3, 5, 9)
    }
}
