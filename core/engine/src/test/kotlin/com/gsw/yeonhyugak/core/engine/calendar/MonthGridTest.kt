package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.d
import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.holidays
import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.work
import com.gsw.yeonhyugak.core.engine.holiday.HolidaySource
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MonthGridTest :
    FunSpec({
        fun grid(ym: YearMonth, leaves: Map<LocalDate, LeaveType> = emptyMap()) =
            MonthGrid.build(ym, holidays, work, leaves)

        fun MonthGrid.at(date: LocalDate): DayCell = cellOf(date).shouldNotBeNull()

        fun MonthGrid.bandAt(date: LocalDate): BlockBand = at(date).band.shouldNotBeNull()

        test("일요일 시작 6주, 이달 날짜만 inMonth") {
            val g = grid(YearMonth.of(2026, 5))
            g.weeks.size shouldBe MonthGrid.WEEKS
            g.weeks.first().first().date shouldBe d(4, 26)
            g.weeks.first().first().dayOfWeek shouldBe DayOfWeek.SUNDAY
            g.days.size shouldBe 31
            g.weeks.flatten().filterNot { it.inMonth }.all {
                it.holidays.isEmpty() && !it.isWorkday
            } shouldBe true
        }

        test("원격 선거일은 번들 공휴일과 같은 규칙으로 표시") {
            val remote = grid(YearMonth.of(2026, 6)).at(d(6, 3))
            remote.holidays.single().source shouldBe HolidaySource.REMOTE
            val bundled = grid(YearMonth.of(2026, 5)).at(d(5, 5))
            listOf(remote, bundled).forEach {
                it.isHoliday shouldBe true
                it.isExpectedHoliday shouldBe false
                it.isWorkday shouldBe false
            }
        }

        test("예상 연도 공휴일만 점선 대상") {
            val seollal = grid(YearMonth.of(2024, 2)).at(d(2, 10, 2024))
            seollal.isHoliday shouldBe true
            seollal.isExpectedHoliday shouldBe true
        }

        test("공휴일+주말 3일 이상이면 연휴 띠, 시작 칸에 라벨") {
            val g = grid(YearMonth.of(2026, 5))
            val fri = g.bandAt(d(5, 1))
            fri.length shouldBe 3
            fri.isStart shouldBe true
            fri.labelHere shouldBe true
            g.bandAt(d(5, 3)).isEnd shouldBe true
            // 주말 2일은 띠 없음
            g.at(d(5, 9)).band.shouldBeNull()
        }

        test("연차로 메우면 블록이 합쳐진다. 반차는 이어주지 않는다") {
            grid(YearMonth.of(2026, 5), mapOf(d(5, 4) to LeaveType.FULL))
                .bandAt(d(5, 1)).length shouldBe 5
            grid(YearMonth.of(2026, 5), mapOf(d(5, 4) to LeaveType.AM_HALF))
                .bandAt(d(5, 1)).length shouldBe 3
        }

        test("지난달에 시작한 블록은 이달 첫 칸에 라벨") {
            val leaves = mapOf(d(4, 30) to LeaveType.FULL)
            grid(YearMonth.of(2026, 5), leaves).bandAt(d(5, 1)).let {
                it.length shouldBe 4
                it.isStart shouldBe false
                it.labelHere shouldBe true
            }
            grid(YearMonth.of(2026, 4), leaves).bandAt(d(4, 30)).labelHere shouldBe true
        }

        test("토요일에서 일요일로 넘어가는 띠는 이어짐 표시") {
            val g = grid(YearMonth.of(2026, 5))
            g.bandAt(d(5, 1)).let {
                it.continuesPrev shouldBe false
                it.continuesNext shouldBe false
            }
            g.bandAt(d(5, 2)).continuesNext shouldBe true
            g.bandAt(d(5, 3)).let {
                it.continuesPrev shouldBe true
                it.continuesNext shouldBe false
            }
        }

        test("달 경계를 넘는 띠도 이어짐 표시") {
            val leaves = mapOf(d(4, 30) to LeaveType.FULL)
            grid(YearMonth.of(2026, 5), leaves).bandAt(d(5, 1)).continuesPrev shouldBe true
            grid(YearMonth.of(2026, 4), leaves).bandAt(d(4, 30)).let {
                it.continuesNext shouldBe true
                it.continuesPrev shouldBe false
            }
        }

        test("지정 기록이 셀에 반영") {
            val cell = grid(YearMonth.of(2026, 5), mapOf(d(5, 7) to LeaveType.PM_HALF)).at(d(5, 7))
            cell.leave shouldBe LeaveType.PM_HALF
            cell.isWorkday shouldBe true
        }
    })
