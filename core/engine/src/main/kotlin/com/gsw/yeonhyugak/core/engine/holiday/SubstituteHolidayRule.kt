package com.gsw.yeonhyugak.core.engine.holiday

import java.time.DayOfWeek
import java.time.LocalDate

// 대체공휴일 대상 묶음
enum class SubstituteGroup { SEOL_CHUSEOK, CHILDREN, NATIONAL_DAYS, BUDDHA_CHRISTMAS, LABOR_DAY }

data class SubstituteScope(
    val group: SubstituteGroup,
    val effectiveFrom: LocalDate,
    val triggerDays: Set<DayOfWeek>,
)

// 관공서의 공휴일에 관한 규정 제3조. 적용 범위는 데이터로 관리
object SubstituteHolidayRule {
    private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    val scopes: List<SubstituteScope> =
        listOf(
            // 설·추석은 일요일만 (토요일 제외)
            SubstituteScope(
                SubstituteGroup.SEOL_CHUSEOK,
                LocalDate.of(2014, 1, 1),
                setOf(DayOfWeek.SUNDAY),
            ),
            SubstituteScope(SubstituteGroup.CHILDREN, LocalDate.of(2014, 1, 1), WEEKEND),
            // 삼일절·광복절·개천절·한글날. 2021 광복절부터. 제헌절은 2026 지정부터
            SubstituteScope(SubstituteGroup.NATIONAL_DAYS, LocalDate.of(2021, 8, 4), WEEKEND),
            SubstituteScope(SubstituteGroup.BUDDHA_CHRISTMAS, LocalDate.of(2023, 5, 4), WEEKEND),
            // 노동절. 2026 지정부터
            SubstituteScope(SubstituteGroup.LABOR_DAY, LocalDate.of(2026, 1, 1), WEEKEND),
        )

    const val NAME = HolidayNames.SUBSTITUTE

    data class Base(val date: LocalDate, val name: String, val group: SubstituteGroup?)

    // 원래 공휴일 목록으로 대체공휴일 날짜를 만든다
    fun substitutes(base: List<Base>): List<LocalDate> {
        val taken = base.map { it.date }.toMutableSet()
        val result = mutableListOf<LocalDate>()
        base.groupBy { it.date }.toSortedMap().forEach { (date, onDate) ->
            repeat(countFor(date, onDate)) {
                val sub = nextFreeWeekday(date, taken)
                taken += sub
                result += sub
            }
        }
        return result.sorted()
    }

    // 주말 겹침은 대상마다 하나, 공휴일끼리 겹침은 (개수 - 1)개. 둘 중 큰 쪽
    private fun countFor(date: LocalDate, onDate: List<Base>): Int {
        val eligible = onDate.mapNotNull { b -> b.group?.let { scopeOf(it, date) } }
        if (eligible.isEmpty()) return 0
        val byWeekend = eligible.count { date.dayOfWeek in it.triggerDays }
        val byOverlap = onDate.size - 1
        return maxOf(byWeekend, byOverlap)
    }

    private fun scopeOf(group: SubstituteGroup, date: LocalDate): SubstituteScope? =
        scopes.firstOrNull {
            it.group == group && !date.isBefore(it.effectiveFrom)
        }

    private fun nextFreeWeekday(from: LocalDate, taken: Set<LocalDate>): LocalDate {
        var d = from.plusDays(1)
        while (d.dayOfWeek in WEEKEND || d in taken) d = d.plusDays(1)
        return d
    }
}
