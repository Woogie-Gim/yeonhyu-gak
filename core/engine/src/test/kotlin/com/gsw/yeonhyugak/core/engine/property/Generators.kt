package com.gsw.yeonhyugak.core.engine.property

import com.gsw.yeonhyugak.core.engine.SupportedYears
import com.gsw.yeonhyugak.core.engine.holiday.LunarDates
import com.gsw.yeonhyugak.core.engine.planner.LeaveBudget
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.subsequence
import java.time.DayOfWeek
import java.time.LocalDate

// 속성 테스트 공용 입력
data class PlanInput(val year: Int, val calendar: WorkCalendar, val budget: LeaveBudget)

object Generators {
    const val ITERATIONS = 1_000

    private fun datesAround(year: Int): Arb<LocalDate> =
        Arb.localDate(LocalDate.of(year - 1, 11, 1), LocalDate.of(year + 1, 2, 28))

    val planInput: Arb<PlanInput> =
        arbitrary {
            val year = Arb.int(SupportedYears.range).bind()
            val workWeek = Arb.subsequence(DayOfWeek.entries).bind().toSet().ifEmpty {
                WorkCalendar.WEEKDAYS
            }
            val holidays = Arb.set(datesAround(year), 0..30).bind()
            val companyOff = Arb.set(datesAround(year), 0..5).bind()
            val booked = Arb.set(datesAround(year), 0..10).bind()
            val halves = Arb.int(0..50).bind()
            val nextYear = Arb.double(0.0, 25.0).bind()
            val budget = LeaveBudget(year, mapOf(year to halves / 2.0, year + 1 to nextYear))
            PlanInput(year, WorkCalendar(holidays, workWeek, companyOff, booked), budget)
        }

    // 실제 범위 안의 임의 음력 공휴일 양력 날짜
    val lunarYear: Arb<Pair<Int, LunarDates>> =
        arbitrary {
            val year = Arb.int(SupportedYears.range).bind()
            val seollal = Arb.localDate(LocalDate.of(year, 1, 21), LocalDate.of(year, 2, 20)).bind()
            val buddha = Arb.localDate(LocalDate.of(year, 4, 20), LocalDate.of(year, 5, 31)).bind()
            val chuseok = Arb.localDate(LocalDate.of(year, 9, 7), LocalDate.of(year, 10, 8)).bind()
            year to LunarDates(seollal, buddha, chuseok)
        }

    val hireAndDate: Arb<Pair<LocalDate, LocalDate>> =
        arbitrary {
            val hire = Arb.localDate(LocalDate.of(1990, 1, 1), LocalDate.of(2035, 12, 31)).bind()
            val on = Arb.localDate(hire, hire.plusYears(40)).bind()
            hire to on
        }
}
