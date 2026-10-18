package com.gsw.yeonhyugak.core.engine.planner

import com.gsw.yeonhyugak.core.engine.holiday.ExpectedHolidayGenerator
import com.gsw.yeonhyugak.core.engine.holiday.KnownLunarDates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId

private fun d(m: Int, day: Int, y: Int = 2025) = LocalDate.of(y, m, day)

class VacationPlannerTest :
    FunSpec({
        // 2025 실제 공휴일 일부 + 1/27 임시공휴일 + 2026 신정
        val holidays =
            ExpectedHolidayGenerator.generate(2025, KnownLunarDates.Y2025).map { it.date }.toSet() +
                d(1, 27) + d(1, 1, 2026)
        val cal = WorkCalendar(holidays)
        val clock = Clock.fixed(
            d(10, 18).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"),
        )
        val planner = VacationPlanner(clock)

        test("주말 포함 5일을 그으면 근무일 3일만 차감") {
            cal.leaveDaysIn(d(2, 7), d(2, 11)) shouldBe listOf(d(2, 7), d(2, 10), d(2, 11))
        }

        test("공휴일 위 구간은 차감 없음") {
            cal.leaveDaysIn(d(1, 27), d(1, 30)).shouldBeEmpty()
        }

        test("효율 1위: 10/10 하루로 10/3~10/12 10일, 설은 1/31 하루로 9일") {
            val list = planner.candidates(2025, cal, LeaveBudget(2025, 15.0))
            list.first() shouldBe VacationBlock(d(10, 3), d(10, 12), listOf(d(10, 10)))
            list.first().efficiency shouldBe 10.0
            list shouldContain VacationBlock(d(1, 25), d(2, 2), listOf(d(1, 31)))
        }

        test("연도 경계 블록은 두 연도에서 따로 차감") {
            val boundary =
                VacationBlock(
                    d(12, 25),
                    d(1, 4, 2026),
                    listOf(d(12, 26), d(12, 29), d(12, 30), d(12, 31), d(1, 2, 2026)),
                )
            planner.candidates(2025, cal, LeaveBudget(2025, 15.0)) shouldContain boundary
            boundary.costByYear shouldBe mapOf(2025 to 4, 2026 to 1)
            boundary.spansYears shouldBe true
        }

        test("다음 해 한도가 0이면 경계 블록 제외, 모르면 올해 한도와 같게") {
            val boundary =
                VacationBlock(
                    d(12, 25),
                    d(1, 4, 2026),
                    listOf(d(12, 26), d(12, 29), d(12, 30), d(12, 31), d(1, 2, 2026)),
                )
            planner.candidates(
                2025,
                cal,
                LeaveBudget(2025, mapOf(2025 to 15.0, 2026 to 0.0)),
            ) shouldNotContain
                boundary
            planner.candidates(2025, cal, LeaveBudget(2025, mapOf(2025 to 4.0))) shouldNotContain
                boundary
            planner.candidates(2025, cal, LeaveBudget(2025, mapOf(2025 to 5.0))) shouldContain
                boundary
        }

        test("반차 단위 남은 연차는 내림 (2.5 → 2)") {
            LeaveBudget(2025, 2.5).limitFor(2025) shouldBe 2
            planner.candidates(2025, cal, LeaveBudget(2025, 2.5)).maxOf { it.cost } shouldBe 2
        }

        test("남은 연차 0: 후보 없음, 연차 없이 쉬는 3일+ 구간 제공") {
            planner.candidates(2025, cal, LeaveBudget(2025, 0.0)).shouldBeEmpty()
            val free = planner.freeBreaks(2025, cal)
            free shouldContain VacationBlock(d(1, 25), d(1, 30), emptyList())
            free shouldContain VacationBlock(d(10, 3), d(10, 9), emptyList())
            free.all { it.totalDays >= 3 && it.cost == 0 } shouldBe true
        }

        test("연간 자동 배치: 겹치지 않고 한도 이내") {
            val plan = planner.autoPlan(2025, cal, LeaveBudget(2025, 10.0))
            plan.sumOf { it.costByYear[2025] ?: 0 } shouldBe 10
            plan.zipWithNext().all { (a, b) -> a.end.isBefore(b.start) } shouldBe true
            plan shouldContain VacationBlock(d(10, 3), d(10, 12), listOf(d(10, 10)))
        }

        test("필터: 최소 길이 · 계절 · 월 · 올해 남은 기간") {
            val budget = LeaveBudget(2025, 15.0)
            planner.candidates(2025, cal, budget, filter = CandidateFilter(minLength = 9)).all {
                it.totalDays >=
                    9
            } shouldBe
                true
            planner.candidates(
                2025,
                cal,
                budget,
                filter = CandidateFilter(season = Season.SUMMER),
            ).all {
                it.start.monthValue in
                    6..8
            } shouldBe
                true
            planner.candidates(2025, cal, budget, filter = CandidateFilter(month = Month.MAY)).all {
                it.start.month ==
                    Month.MAY
            } shouldBe
                true
            val rest = planner.candidates(2025, cal, budget, filter = planner.remainingThisYear())
            rest.all { !it.start.isBefore(d(10, 18)) } shouldBe true
            planner.today() shouldBe d(10, 18)
        }

        test("정렬: 긴 순 · 날짜 순") {
            val budget = LeaveBudget(2025, 5.0)
            val byLength = planner.candidates(2025, cal, budget, SortOrder.LENGTH)
            byLength.zipWithNext().all { (a, b) -> a.totalDays >= b.totalDays } shouldBe true
            val byDate = planner.candidates(2025, cal, budget, SortOrder.DATE)
            byDate.zipWithNext().all { (a, b) -> !a.start.isAfter(b.start) } shouldBe true
        }

        test("이미 지정한 연차는 비근무일로 보고 블록에 합쳐짐") {
            val booked = cal.copy(bookedLeave = setOf(d(1, 31)))
            booked.isWorkday(d(1, 31)) shouldBe false
            planner.candidates(2025, booked, LeaveBudget(2025, 1.0)) shouldContain
                VacationBlock(d(1, 24), d(2, 2), listOf(d(1, 24)))
        }
    })
