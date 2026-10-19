package com.gsw.yeonhyugak.core.engine.planner

import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures
import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.d
import com.gsw.yeonhyugak.core.engine.leave.LeaveLedger
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

fun fixedClock(date: LocalDate): Clock =
    Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

// 2026 픽스처(노동절 5/1 · 어린이날 5/5 · 선거일 6/3). 오늘 = 2026-05-06(수)
class RecommenderTest :
    FunSpec({
        val today = d(5, 6)
        val rec = Recommender(VacationPlanner(fixedClock(today)))
        val work = CalendarFixtures.work
        val rest = RecommendPeriod.REST_OF_YEAR

        test("기본 기간: 오늘 이전 날짜가 들어간 연휴는 없다") {
            val list = rec.recommend(rest, work, emptyMap(), 15.0)
            list.shouldNotBeEmpty()
            list.all { !it.start.isBefore(today) && it.start.year == 2026 } shouldBe true
        }

        test("기존 연차 때문에 구간 시작이 과거가 되면 뺀다") {
            // 5/4(월) 연차 → 오늘(5/6)을 메우는 블록은 5/1부터 이어져 시작이 과거
            val leaves = mapOf(d(5, 4) to LeaveType.FULL)
            val list = rec.recommend(rest, work, leaves, 15.0)
            list.none { d(5, 6) in it.leaveDates } shouldBe true
            list.none { it.start.isBefore(today) } shouldBe true
        }

        test("기존 연차와 붙어 길어지는 블록도 후보") {
            // 6/5(금) 연차 → 6/4(목) 하루로 6/3 선거일~6/7 5일
            val leaves = mapOf(d(6, 5) to LeaveType.FULL)
            val joined = rec.recommend(rest, work, leaves, 15.0)
                .first { it.leaveDates == listOf(d(6, 4)) }
            joined.span shouldBe d(6, 3)..d(6, 7)
            joined.totalDays shouldBe 5
            joined.bookedInside shouldBe 1
        }

        test("이미 쓴 연차만큼 예산에서 뺀다") {
            val leaves = mapOf(d(7, 1) to LeaveType.FULL, d(7, 2) to LeaveType.FULL)
            rec.budget(rest, 3.0, leaves).limitFor(2026) shouldBe 1
            // 연도 경계 블록은 다음 해 연차에서 따로 차감
            rec.recommend(rest, work, leaves, 3.0).all { (it.costByYear[2026] ?: 0) <= 1 } shouldBe
                true
        }

        test("남은 연차를 다 쓰면 후보 없음") {
            val leaves = mapOf(d(7, 1) to LeaveType.FULL, d(7, 2) to LeaveType.FULL)
            rec.recommend(rest, work, leaves, 2.0).shouldBeEmpty()
        }

        test("반차는 연휴를 잇지 않는다") {
            // 6/5 오전 반차는 출근일. 6/4 하루면 6/3~6/4 2일이라 후보 아님
            val leaves = mapOf(d(6, 5) to LeaveType.AM_HALF)
            rec.recommend(rest, work, leaves, 15.0)
                .none { it.leaveDates == listOf(d(6, 4)) } shouldBe true
        }

        test("2일 이하 블록은 후보가 아니다") {
            rec.recommend(rest, work, emptyMap(), 15.0).all { it.totalDays >= 3 } shouldBe true
        }

        test("자동 배치: 겹치지 않고 남은 예산 안") {
            val leaves = mapOf(d(8, 3) to LeaveType.FULL)
            val plan = rec.autoPlan(rest, work, leaves, 5.0)
            plan.shouldNotBeEmpty()
            plan.sumOf { it.cost } shouldBeLessThanOrEqual 4
            plan.zipWithNext().none { (a, b) -> a.overlaps(b) } shouldBe true
        }

        test("자동 배치 블록은 recommend 결과 안에서만") {
            val all = rec.recommend(rest, work, emptyMap(), 15.0)
            rec.autoPlan(rest, work, emptyMap(), 15.0).forEach { all shouldContain it }
        }

        test("남은 연차 0이면 연차 없이 쉬는 날만") {
            // 4/20 기준: 5/1(금) 노동절~5/3 3일
            val april = Recommender(VacationPlanner(fixedClock(d(4, 20))))
            val breaks = april.freeBreaks(rest, work, emptyMap())
            breaks.map { it.span } shouldContain d(5, 1)..d(5, 3)
            breaks.all {
                it.cost == 0 && it.totalDays >= 3 && !it.start.isBefore(d(4, 20))
            } shouldBe
                true
            // 오늘(5/6) 이후에는 3일 이상 쉬는 구간이 없다
            rec.freeBreaks(rest, work, emptyMap()).shouldBeEmpty()
        }

        test("내년 보기: 다음 해에 시작하는 블록만") {
            val next = rec.recommend(RecommendPeriod.NEXT_YEAR, work, emptyMap(), 15.0)
            next.shouldNotBeEmpty()
            next.all { it.start.year == 2027 } shouldBe true
        }

        test("정렬: 효율 순 기본 · 날짜 순") {
            rec.recommend(rest, work, emptyMap(), 15.0)
                .zipWithNext().all { (a, b) -> a.efficiency >= b.efficiency } shouldBe true
            rec.recommend(rest, work, emptyMap(), 15.0, sort = SortOrder.DATE)
                .zipWithNext().all { (a, b) -> !a.start.isAfter(b.start) } shouldBe true
        }

        test("필터: 최소 길이 5일+") {
            val list = rec.recommend(rest, work, emptyMap(), 15.0, CandidateFilter(minLength = 5))
            list.shouldNotBeEmpty()
            list.forEach { it.totalDays shouldBeGreaterThanOrEqual 5 }
        }

        test("남은 개수 = 총량 − 지정분. 반차 0.5 · 초과는 음수") {
            val leaves = mapOf(d(7, 1) to LeaveType.FULL, d(7, 2) to LeaveType.PM_HALF)
            LeaveLedger.remaining(15.0, leaves, 2026) shouldBe 13.5
            LeaveLedger.remaining(1.0, leaves, 2026) shouldBe -0.5
            LeaveLedger.remaining(15.0, leaves, 2027) shouldBe 15.0
        }
    })
