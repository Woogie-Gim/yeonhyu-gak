package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.LocalDate

sealed interface TapResult {
    data class Marked(val date: LocalDate) : TapResult

    data class Cleared(val date: LocalDate, val previous: LeaveType) : TapResult

    // 공휴일·주말·휴무일: 지정 안 함. 공휴일이면 이름 툴팁
    data class NotWorkday(val date: LocalDate) : TapResult
}

// 탭 한 번: 지정돼 있으면 해제, 근무일이면 연차 1일
object LeaveToggle {
    fun onTap(date: LocalDate, work: WorkCalendar, leaves: Map<LocalDate, LeaveType>): TapResult {
        leaves[date]?.let { return TapResult.Cleared(date, it) }
        return if (work.isWorkday(date)) TapResult.Marked(date) else TapResult.NotWorkday(date)
    }
}
