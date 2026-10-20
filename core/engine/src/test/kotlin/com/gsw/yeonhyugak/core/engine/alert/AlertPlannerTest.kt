package com.gsw.yeonhyugak.core.engine.alert

import com.gsw.yeonhyugak.core.engine.calendar.BreakSpan
import com.gsw.yeonhyugak.core.engine.leave.LeaveAccrualCalculator
import com.gsw.yeonhyugak.core.engine.leave.LeaveBasis
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class AlertPlannerTest :
    FunSpec({
        fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)
        val may = BreakSpan(d(5, 1), d(5, 5), 1)

        fun due(today: LocalDate, sent: Set<String> = emptySet(), expiry: LocalDate? = null) =
            AlertPlanner.due(today, listOf(may), expiry, sent)

        test("연휴 시작 21일 전에 보낸다") {
            due(d(4, 10)) shouldContainExactly listOf(Alert.BreakSoon(may, 21))
        }

        test("22일 전은 아직") {
            due(d(4, 9)).shouldBeEmpty()
        }

        test("작업이 늦어도 7일 안이면 남은 일수로 보내고 그 뒤는 안 보낸다") {
            due(d(4, 17)) shouldContainExactly listOf(Alert.BreakSoon(may, 14))
            due(d(4, 18)).shouldBeEmpty()
        }

        test("이미 보낸 키는 다시 안 보낸다") {
            val key = Alert.BreakSoon(may, 21).key
            due(d(4, 11), setOf(key)).shouldBeEmpty()
        }

        test("연휴 날짜가 바뀌면 새 키") {
            Alert.BreakSoon(may, 21).key shouldBe "break:2026-05-01:2026-05-05"
            Alert.BreakSoon(may.copy(end = d(5, 6)), 21).key shouldBe "break:2026-05-01:2026-05-06"
        }

        test("연차 소멸 30일 전") {
            due(d(5, 2), expiry = d(6, 1)) shouldContainExactly
                listOf(Alert.LeaveExpiring(d(6, 1), 30))
        }

        test("입사일 기준 소멸일 = 첫 부여분 1주년") {
            LeaveAccrualCalculator.nextExpiry(
                LocalDate.of(2025, 3, 2),
                LeaveBasis.ANNIVERSARY,
                LocalDate.of(2025, 10, 20),
            ) shouldBe LocalDate.of(2026, 3, 2)
        }
    })
