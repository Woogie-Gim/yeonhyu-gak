package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.d
import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.work
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class RangeSelectionTest :
    FunSpec({
        fun sum(
            from: LocalDate,
            to: LocalDate,
            leaves: Map<LocalDate, LeaveType> = emptyMap(),
            allowance: Double = 15.0,
            cal: WorkCalendar = work,
        ) = RangeSelection.summarize(from, to, cal, leaves, allowance)

        test("주말 포함 5일을 그으면 3일만 차감") {
            val s = sum(d(5, 7), d(5, 11))
            s.leaveDates shouldBe listOf(d(5, 7), d(5, 8), d(5, 11))
            s.cost shouldBe 3.0
            s.totalDays shouldBe 5
            s.isOver shouldBe false
        }

        test("공휴일은 차감하지 않고 이어진 휴일까지 총 일수에 포함") {
            val s = sum(d(4, 30), d(5, 4))
            s.leaveDates shouldBe listOf(d(4, 30), d(5, 4))
            // 4/30 ~ 5/5 어린이날
            s.totalDays shouldBe 6
        }

        test("거꾸로 그어도 같은 결과") {
            sum(d(5, 11), d(5, 7)) shouldBe sum(d(5, 7), d(5, 11))
        }

        test("이미 연차인 날은 다시 차감 안 함, 반차는 남은 반만 차감") {
            val leaves = mapOf(d(5, 7) to LeaveType.FULL, d(5, 8) to LeaveType.AM_HALF)
            val s = sum(d(5, 7), d(5, 11), leaves)
            s.leaveDates shouldBe listOf(d(5, 8), d(5, 11))
            s.cost shouldBe 1.5
        }

        test("남은 연차 초과는 막지 않고 초과분만 알려준다") {
            val s = sum(d(5, 7), d(5, 11), mapOf(d(3, 2) to LeaveType.FULL), allowance = 2.0)
            s.leaveDates.size shouldBe 3
            s.overBy shouldBe 2.0
        }

        test("이미 초과한 상태면 새로 차감하는 만큼 전부 초과") {
            sum(d(5, 7), d(5, 8), mapOf(d(3, 2) to LeaveType.FULL), allowance = 0.5)
                .overBy shouldBe 2.0
        }

        test("연도 경계는 연도별로 따로 초과 계산") {
            val cal = WorkCalendar(holidays = emptySet())
            // 2026-12-30(수) ~ 2027-01-01(금), 연도별 허용 1일
            val s = sum(d(12, 30), LocalDate.of(2027, 1, 1), allowance = 1.0, cal = cal)
            s.cost shouldBe 3.0
            s.overBy shouldBe 1.0
        }

        test("회사 휴무일도 차감하지 않는다") {
            val cal = work.copy(companyOffDays = setOf(d(5, 8)))
            sum(d(5, 7), d(5, 8), cal = cal).leaveDates shouldBe listOf(d(5, 7))
        }
    })
