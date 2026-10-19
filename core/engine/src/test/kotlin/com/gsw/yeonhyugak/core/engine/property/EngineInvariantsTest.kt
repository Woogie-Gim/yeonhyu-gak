package com.gsw.yeonhyugak.core.engine.property

import com.gsw.yeonhyugak.core.engine.calendar.RangeSelection
import com.gsw.yeonhyugak.core.engine.calendar.VacationSpan
import com.gsw.yeonhyugak.core.engine.holiday.ExpectedHolidayGenerator
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.leave.AccrualResult
import com.gsw.yeonhyugak.core.engine.leave.LeaveAccrualCalculator
import com.gsw.yeonhyugak.core.engine.leave.LeaveBasis
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.RecommendPeriod
import com.gsw.yeonhyugak.core.engine.planner.Recommender
import com.gsw.yeonhyugak.core.engine.planner.VacationPlanner
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// SPEC 13장 엔진 불변식 + 달력 구간 불변식. 각 1,000회
class EngineInvariantsTest :
    FunSpec({
        val planner =
            VacationPlanner(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC))
        val n = Generators.ITERATIONS

        test("1. 추천 블록의 비용 ≤ 남은 연차 (연도별)") {
            checkAll(n, Generators.planInput) { (year, cal, budget) ->
                planner.candidates(year, cal, budget).forEach { b ->
                    b.costByYear.forEach { (y, c) ->
                        c.toDouble() shouldBeLessThanOrEqual
                            (budget.remainingByYear[y] ?: budget.remainingByYear.getValue(year))
                    }
                }
            }
        }

        test("2. 연차는 비근무일에 절대 배치되지 않는다") {
            checkAll(n, Generators.planInput) { (year, cal, budget) ->
                (planner.candidates(year, cal, budget) + planner.autoPlan(year, cal, budget))
                    .flatMap { it.leaveDates }
                    .all(cal::isWorkday) shouldBe true
            }
        }

        test("3. 총 휴일 수 ≥ 비용") {
            checkAll(n, Generators.planInput) { (year, cal, budget) ->
                planner.candidates(year, cal, budget).forEach {
                    it.totalDays shouldBeGreaterThanOrEqual
                        it.cost
                }
            }
        }

        test("4. 추천 블록은 극대다 (양 끝 바로 바깥이 근무일, 안쪽 근무일은 전부 연차)") {
            checkAll(n, Generators.planInput) { (year, cal, budget) ->
                planner.candidates(year, cal, budget).forEach { b ->
                    cal.isWorkday(b.start.minusDays(1)) shouldBe true
                    cal.isWorkday(b.end.plusDays(1)) shouldBe true
                    cal.leaveDaysIn(b.start, b.end) shouldBe b.leaveDates
                }
            }
        }

        test("5. 연간 자동 배치 블록끼리 겹치지 않는다") {
            checkAll(n, Generators.planInput) { (year, cal, budget) ->
                val plan = planner.autoPlan(year, cal, budget)
                plan.indices.all { i ->
                    (i + 1 until plan.size).none { j -> plan[i].overlaps(plan[j]) }
                } shouldBe
                    true
                plan.flatMap {
                    it.leaveDates
                }.groupingBy { it.year }.eachCount().forEach { (y, c) ->
                    c shouldBeLessThanOrEqual budget.limitFor(y)
                }
            }
        }

        test("6. 연도 경계 블록의 비용은 두 연도 합과 같다") {
            checkAll(n, Generators.planInput) { (year, cal, budget) ->
                planner.candidates(year, cal, budget).filter { it.spansYears }.forEach { b ->
                    b.cost shouldBe b.costByYear.values.sum()
                    b.cost shouldBe
                        (b.costByYear[b.start.year] ?: 0) + (b.costByYear[b.end.year] ?: 0)
                }
            }
        }

        test("7. 대체공휴일은 원래 공휴일과 같은 날이 아니다") {
            checkAll(n, Generators.lunarYear) { (year, lunar) ->
                val hs = ExpectedHolidayGenerator.generate(year, lunar)
                val originals = hs.filter {
                    it.kind != HolidayKind.SUBSTITUTE
                }.map { it.date }.toSet()
                hs.filter { it.kind == HolidayKind.SUBSTITUTE }.forEach { s ->
                    (s.date in originals) shouldBe false
                    (s.date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) shouldBe false
                }
            }
        }

        test("8. 연차 계산값은 0 이상 25 이하") {
            checkAll(n, Generators.hireAndDate, Arb.enum<LeaveBasis>()) { (hire, on), basis ->
                val r = LeaveAccrualCalculator.calculate(
                    hire,
                    on,
                    basis,
                ) as AccrualResult.Calculated
                r.grants.forEach {
                    it.days shouldBeGreaterThanOrEqual 0.0
                    it.days shouldBeLessThanOrEqual 25.0
                }
                if (basis == LeaveBasis.ANNIVERSARY) {
                    r.entitlementOn(on) shouldBeGreaterThanOrEqual 0.0
                    r.entitlementOn(on) shouldBeLessThanOrEqual 25.0
                }
            }
        }

        test("9. 달력에 그은 구간의 차감일은 전부 근무일, 총 일수 ≥ 구간 길이") {
            checkAll(n, Generators.planInput, Arb.int(0..20)) { (year, cal, _), len ->
                val start = LocalDate.of(year, 3, 1)
                val end = start.plusDays(len.toLong())
                val s = RangeSelection.summarize(start, end, cal, emptyMap(), 15.0)
                s.leaveDates.all(cal::isWorkday) shouldBe true
                s.leaveDates.all { it in start..end } shouldBe true
                s.totalDays shouldBeGreaterThanOrEqual len + 1
                s.overBy shouldBeLessThanOrEqual s.cost
            }
        }

        test("10. 추천 카드 구간 = 담은 뒤 달력 연휴 구간, 비용 ≤ 이미 쓴 만큼 뺀 연차") {
            checkAll(n, Generators.planInput) { (year, cal, budget) ->
                val clock = Clock.fixed(
                    LocalDate.of(year, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
                    ZoneOffset.UTC,
                )
                val rec = Recommender(VacationPlanner(clock))
                // 생성된 bookedLeave를 달력 연차로 옮긴다
                val leaves = cal.bookedLeave.associateWith { LeaveType.FULL }
                val work = cal.copy(bookedLeave = emptySet())
                val allowance = budget.remainingByYear.getValue(year)
                val usedThisYear = leaves.keys.count { it.year == year }
                rec.recommend(RecommendPeriod.REST_OF_YEAR, work, leaves, allowance).forEach { r ->
                    val after = leaves + r.leaveDates.associateWith { LeaveType.FULL }
                    VacationSpan.around(r.leaveDates.first(), work, after) shouldBe r.span
                    r.totalDays shouldBeGreaterThanOrEqual VacationSpan.MIN_DAYS
                    (r.costByYear[year] ?: 0).toDouble() shouldBeLessThanOrEqual
                        (allowance - usedThisYear).coerceAtLeast(0.0)
                }
            }
        }
    })
