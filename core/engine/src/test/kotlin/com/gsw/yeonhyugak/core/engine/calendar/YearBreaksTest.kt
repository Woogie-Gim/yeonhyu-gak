package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.d
import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.work
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

// 공유 카드 연휴 목록 · 위젯 D-n
class YearBreaksTest :
    FunSpec({
        test("연차 없으면 노동절 금~일 3일만 연휴") {
            val y = YearBreakCalculator.of(2026, work, emptyMap())
            y.spans shouldContain BreakSpan(d(5, 1), d(5, 3), 0)
            y.spans.none { it.contains(d(5, 5)) } shouldBe true
            y.usedLeave shouldBe 0.0
        }

        test("5/4 연차로 5일 연휴. 반차는 이어 붙이지 않고 0.5만 더한다") {
            val leaves = mapOf(d(5, 4) to LeaveType.FULL, d(6, 2) to LeaveType.AM_HALF)
            val y = YearBreakCalculator.of(2026, work, leaves)
            val may = y.spans.single { it.contains(d(5, 4)) }
            may shouldBe BreakSpan(d(5, 1), d(5, 5), 1)
            may.days shouldBe 5
            y.usedLeave shouldBe 1.5
            y.spans.none { it.contains(d(6, 2)) } shouldBe true
            YearBreakCalculator.planned(2026, work, leaves) shouldContainExactly listOf(may)
        }

        test("총 휴일 수 = 구간 일수 합") {
            val y = YearBreakCalculator.of(2026, work, mapOf(d(5, 4) to LeaveType.FULL))
            y.totalDays shouldBe y.spans.sumOf { it.days }
        }

        test("카드 구간: 내 연차 연휴가 있으면 그것만, 없으면 전체") {
            val none = YearBreakCalculator.of(2026, work, emptyMap())
            none.highlights() shouldBe none
            val mine = YearBreakCalculator.of(
                2026,
                work,
                mapOf(d(5, 4) to LeaveType.FULL),
            ).highlights()
            mine.spans shouldContainExactly listOf(BreakSpan(d(5, 1), d(5, 5), 1))
            mine.totalDays shouldBe 5
        }

        test("다음 연휴까지 D-n") {
            val next = NextBreakFinder.from(d(4, 20), work, emptyMap())!!
            next.span shouldBe BreakSpan(d(5, 1), d(5, 3), 0)
            next.daysLeft shouldBe 11
            next.ongoing shouldBe false
        }

        test("연휴 안이면 D-0 진행 중") {
            val next = NextBreakFinder.from(d(5, 2), work, emptyMap())!!
            next.ongoing shouldBe true
            next.span.start shouldBe d(5, 1)
        }

        test("범위 끝까지 연휴가 없으면 null") {
            NextBreakFinder.from(d(12, 1), work, emptyMap()).shouldBeNull()
        }
    })
