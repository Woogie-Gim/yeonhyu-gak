package com.gsw.yeonhyugak.core.engine.planner

import com.gsw.yeonhyugak.core.engine.leave.LeaveLedger
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.time.LocalDate
import kotlin.math.floor

// 연도별 남은 연차. 모르는 연도는 계획 연도와 같다고 가정
data class LeaveBudget(val planYear: Int, val remainingByYear: Map<Int, Double>) {
    constructor(planYear: Int, remaining: Double) : this(planYear, mapOf(planYear to remaining))

    // 추천은 반차를 쓰지 않으므로 정수 일만
    fun limitFor(year: Int): Int {
        val remaining = remainingByYear[year] ?: remainingByYear[planYear] ?: 0.0
        return floor(remaining.coerceAtLeast(0.0)).toInt()
    }

    companion object {
        // 연도별 총량에서 달력에 이미 지정한 만큼 뺀 예산. 총량은 모든 연도 같다고 가정
        fun afterUsed(
            planYear: Int,
            allowance: Double,
            leaves: Map<LocalDate, LeaveType>,
        ): LeaveBudget = LeaveBudget(
            planYear,
            (planYear - 1..planYear + 1).associateWith {
                LeaveLedger.remaining(allowance, leaves, it)
            },
        )
    }
}
