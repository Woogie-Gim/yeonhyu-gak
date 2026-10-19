package com.gsw.yeonhyugak.core.engine.calendar

import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.d
import com.gsw.yeonhyugak.core.engine.calendar.CalendarFixtures.work
import com.gsw.yeonhyugak.core.engine.leave.LeaveLedger
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LeaveToggleTest :
    FunSpec({
        test("근무일 탭은 연차 지정") {
            LeaveToggle.onTap(d(5, 7), work, emptyMap()) shouldBe TapResult.Marked(d(5, 7))
        }

        test("지정된 날 탭은 해제. 반차도 해제") {
            LeaveToggle.onTap(d(5, 7), work, mapOf(d(5, 7) to LeaveType.AM_HALF)) shouldBe
                TapResult.Cleared(d(5, 7), LeaveType.AM_HALF)
        }

        test("공휴일·원격 선거일·주말 탭은 지정 안 됨") {
            listOf(d(5, 5), d(6, 3), d(5, 9)).forEach {
                LeaveToggle.onTap(it, work, emptyMap()) shouldBe TapResult.NotWorkday(it)
            }
        }

        test("사용량은 연차 1 · 반차 0.5로 연도별 합계") {
            val leaves = mapOf(
                d(5, 7) to LeaveType.FULL,
                d(5, 8) to LeaveType.AM_HALF,
                d(5, 11) to LeaveType.PM_HALF,
                d(1, 4, 2027) to LeaveType.FULL,
            )
            LeaveLedger.used(leaves, 2026) shouldBe 2.0
            LeaveLedger.usedByYear(leaves) shouldBe mapOf(2026 to 2.0, 2027 to 1.0)
        }
    })
