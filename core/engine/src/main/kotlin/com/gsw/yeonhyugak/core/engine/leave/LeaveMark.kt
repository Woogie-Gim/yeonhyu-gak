package com.gsw.yeonhyugak.core.engine.leave

import java.time.LocalDate

// 연차 1일 · 오전 반차 · 오후 반차
enum class LeaveType(val amount: Double) {
    FULL(FULL_DAY),
    AM_HALF(HALF_DAY),
    PM_HALF(HALF_DAY),
}

private const val FULL_DAY = 1.0
private const val HALF_DAY = 0.5

// 날짜별 지정 기록 합계
object LeaveLedger {
    fun used(leaves: Map<LocalDate, LeaveType>, year: Int): Double =
        leaves.entries.filter { it.key.year == year }.sumOf { it.value.amount }

    fun usedByYear(leaves: Map<LocalDate, LeaveType>): Map<Int, Double> =
        leaves.entries.groupBy { it.key.year }.mapValues { (_, e) -> e.sumOf { it.value.amount } }
}
