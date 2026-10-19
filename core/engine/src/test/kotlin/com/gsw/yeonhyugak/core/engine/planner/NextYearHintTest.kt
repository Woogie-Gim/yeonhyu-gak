package com.gsw.yeonhyugak.core.engine.planner

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

// "내년 보기" 칩 강조: Clock 주입으로 11월 이후를 흉내
class NextYearHintTest :
    FunSpec({
        fun hint(date: LocalDate) = VacationPlanner(fixedClock(date)).suggestsNextYear()

        test("10월 31일까지는 강조 안 함") {
            hint(LocalDate.of(2026, 10, 31)) shouldBe false
            hint(LocalDate.of(2026, 1, 1)) shouldBe false
        }

        test("11월 1일부터 12월 31일까지 강조") {
            hint(LocalDate.of(2026, 11, 1)) shouldBe true
            hint(LocalDate.of(2026, 12, 31)) shouldBe true
        }

        test("Recommender도 같은 값") {
            Recommender(VacationPlanner(fixedClock(LocalDate.of(2026, 11, 2))))
                .suggestsNextYear() shouldBe true
        }
    })
