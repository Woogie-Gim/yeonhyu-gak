package com.gsw.yeonhyugak.core.engine.planner

import java.time.DayOfWeek
import java.time.LocalDate

// 근무일 = 근무 요일 ∧ 공휴일 아님 ∧ 회사 휴무일 아님 ∧ 이미 연차 아님
data class WorkCalendar(
    val holidays: Set<LocalDate>,
    val workWeek: Set<DayOfWeek> = WEEKDAYS,
    val companyOffDays: Set<LocalDate> = emptySet(),
    val bookedLeave: Set<LocalDate> = emptySet(),
) {
    fun isWorkday(date: LocalDate): Boolean = date.dayOfWeek in workWeek &&
        date !in holidays &&
        date !in companyOffDays &&
        date !in bookedLeave

    // 그은 구간 중 연차로 차감할 날짜만
    fun leaveDaysIn(start: LocalDate, endInclusive: LocalDate): List<LocalDate> =
        start.datesUntil(endInclusive.plusDays(1)).filter(::isWorkday).toList()

    companion object {
        val WEEKDAYS: Set<DayOfWeek> = DayOfWeek.entries.filter {
            it != DayOfWeek.SATURDAY &&
                it != DayOfWeek.SUNDAY
        }.toSet()
    }
}
