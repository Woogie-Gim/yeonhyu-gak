package com.gsw.yeonhyugak.core.engine.leave

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

private fun entitlement(
    hire: LocalDate,
    on: LocalDate,
    basis: LeaveBasis = LeaveBasis.ANNIVERSARY,
): Double {
    val result = LeaveAccrualCalculator.calculate(hire, on, basis)
    result.shouldBeInstanceOf<AccrualResult.Calculated>()
    return result.entitlementOn(on)
}

class LeaveAccrualCalculatorTest :
    FunSpec({
        val hire = LocalDate.of(2024, 1, 15)

        test("입사 11개월: 월 개근 11일") {
            entitlement(hire, LocalDate.of(2024, 12, 15)) shouldBe 11.0
        }

        test("입사 12개월 전날까지 11일, 당일 15일 (1년 미만분 소멸)") {
            entitlement(hire, LocalDate.of(2025, 1, 14)) shouldBe 11.0
            entitlement(hire, LocalDate.of(2025, 1, 15)) shouldBe 15.0
        }

        test("입사 1개월 전날 0일, 당일 1일") {
            entitlement(hire, LocalDate.of(2024, 2, 14)) shouldBe 0.0
            entitlement(hire, LocalDate.of(2024, 2, 15)) shouldBe 1.0
        }

        test("2년 15일 · 3년 16일 · 5년 17일") {
            entitlement(hire, LocalDate.of(2026, 1, 15)) shouldBe 15.0
            entitlement(hire, LocalDate.of(2027, 1, 15)) shouldBe 16.0
            entitlement(hire, LocalDate.of(2029, 1, 15)) shouldBe 17.0
        }

        test("21년 이상은 25일 상한") {
            LeaveAccrualCalculator.annualDays(21) shouldBe 25
            LeaveAccrualCalculator.annualDays(30) shouldBe 25
            entitlement(LocalDate.of(2000, 3, 2), LocalDate.of(2021, 3, 2)) shouldBe 25.0
        }

        test("윤년 2/29 입사: 평년 주년은 2/28, 윤년 주년은 2/29") {
            val leap = LocalDate.of(2024, 2, 29)
            entitlement(leap, LocalDate.of(2025, 2, 27)) shouldBe 11.0
            entitlement(leap, LocalDate.of(2025, 2, 28)) shouldBe 15.0
            val r = LeaveAccrualCalculator.calculate(
                leap,
                LocalDate.of(2028, 3, 1),
                LeaveBasis.ANNIVERSARY,
            ) as AccrualResult.Calculated
            r.grants.last().grantedOn shouldBe LocalDate.of(2028, 2, 29)
        }

        test("회계연도: 다음 해 1/1 비례 부여 0.5일 단위 올림") {
            // 15 × 122/365 = 5.01 → 5.5
            LeaveAccrualCalculator.proratedDays(LocalDate.of(2025, 9, 1)) shouldBe 5.5
            // 15 × 92/365 = 3.78 → 4.0
            LeaveAccrualCalculator.proratedDays(LocalDate.of(2025, 10, 1)) shouldBe 4.0
            LeaveAccrualCalculator.proratedDays(LocalDate.of(2025, 1, 1)) shouldBe 15.0
            LeaveAccrualCalculator.proratedDays(LocalDate.of(2025, 12, 31)) shouldBe 0.5
        }

        test("회계연도: 이후 1/1마다 만근속 연수 기준") {
            val h = LocalDate.of(2025, 9, 1)
            val r = LeaveAccrualCalculator.calculate(
                h,
                LocalDate.of(2029, 1, 1),
                LeaveBasis.FISCAL_YEAR,
            ) as AccrualResult.Calculated
            r.grants.filter { !it.monthly }.map { it.grantedOn.year to it.days } shouldBe
                listOf(2026 to 5.5, 2027 to 15.0, 2028 to 15.0, 2029 to 16.0)
        }

        test("5인 미만은 직접 입력 안내") {
            LeaveAccrualCalculator.calculate(
                hire,
                LocalDate.of(2026, 1, 1),
                LeaveBasis.ANNIVERSARY,
                smallBusiness = true,
            ) shouldBe
                AccrualResult.ManualInputRequired
        }
    })
