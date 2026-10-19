package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayCalendar
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

// 연휴 띠 조각. labelHere = 이 칸에 `n일` 라벨
data class BlockBand(
    val length: Int,
    val isStart: Boolean,
    val isEnd: Boolean,
    val labelHere: Boolean,
)

data class DayCell(
    val date: LocalDate,
    val inMonth: Boolean,
    val holidays: List<Holiday>,
    val leave: LeaveType?,
    // 연차를 지정할 수 있는 날
    val isWorkday: Boolean,
    val band: BlockBand?,
) {
    val isHoliday: Boolean get() = holidays.isNotEmpty()

    // 표시 규칙은 확정 여부만 본다. 출처(번들·원격)는 보지 않는다
    val isExpectedHoliday: Boolean get() = holidays.isNotEmpty() && holidays.none { it.confirmed }

    val dayOfWeek: DayOfWeek get() = date.dayOfWeek
}

data class MonthGrid(val month: YearMonth, val weeks: List<List<DayCell>>) {
    val days: List<DayCell> get() = weeks.flatten().filter { it.inMonth }

    fun cellOf(date: LocalDate): DayCell? = days.firstOrNull { it.date == date }

    companion object {
        const val WEEKS = 6
        const val MIN_BLOCK_DAYS = 3
        private const val DAYS_IN_WEEK = 7

        // 일요일 시작 6주 그리드. work에는 bookedLeave를 넣지 않는다
        fun build(
            month: YearMonth,
            holidays: HolidayCalendar,
            work: WorkCalendar,
            leaves: Map<LocalDate, LeaveType>,
        ): MonthGrid {
            val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            val off = DayOff(work, leaves)
            val cells = (0 until WEEKS * DAYS_IN_WEEK).map { i ->
                val date = first.plusDays(i.toLong())
                val inMonth = YearMonth.from(date) == month
                DayCell(
                    date = date,
                    inMonth = inMonth,
                    holidays = if (inMonth) holidays.on(date) else emptyList(),
                    leave = if (inMonth) leaves[date] else null,
                    isWorkday = inMonth && work.isWorkday(date),
                    band = if (inMonth) bandOf(date, month, off) else null,
                )
            }
            return MonthGrid(month, cells.chunked(DAYS_IN_WEEK))
        }

        private fun bandOf(date: LocalDate, month: YearMonth, off: DayOff): BlockBand? {
            val run = off.runAround(date)
            val length =
                run?.let { ChronoUnit.DAYS.between(it.start, it.endInclusive).toInt() + 1 } ?: 0
            // 블록이 지난달에 시작했으면 이달 첫 칸에 라벨
            return run?.takeIf { length >= MIN_BLOCK_DAYS }?.let {
                BlockBand(
                    length = length,
                    isStart = date == it.start,
                    isEnd = date == it.endInclusive,
                    labelHere = date == maxOf(it.start, month.atDay(1)),
                )
            }
        }
    }
}
