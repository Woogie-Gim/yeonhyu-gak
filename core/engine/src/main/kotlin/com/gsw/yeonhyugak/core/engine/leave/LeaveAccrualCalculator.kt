package com.gsw.yeonhyugak.core.engine.leave

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class LeaveBasis { ANNIVERSARY, FISCAL_YEAR }

// 부여일부터 소멸일 전날까지 유효
data class LeaveGrant(
    val grantedOn: LocalDate,
    val days: Double,
    val expiresOn: LocalDate,
    val monthly: Boolean,
) {
    fun isValidOn(date: LocalDate): Boolean = !date.isBefore(grantedOn) && date.isBefore(expiresOn)
}

sealed interface AccrualResult {
    data class Calculated(val grants: List<LeaveGrant>) : AccrualResult {
        fun entitlementOn(date: LocalDate): Double =
            grants.filter { it.isValidOn(date) }.sumOf { it.days }
    }

    // 5인 미만: 회사 규정에 따라 직접 입력
    data object ManualInputRequired : AccrualResult
}

// 근로기준법 제60조 기준 참고용 계산. 80% 이상 출근 가정
object LeaveAccrualCalculator {
    const val MONTHLY_MAX = 11
    const val BASE_DAYS = 15
    const val MAX_DAYS = 25

    fun calculate(
        hireDate: LocalDate,
        until: LocalDate,
        basis: LeaveBasis,
        smallBusiness: Boolean = false,
    ): AccrualResult {
        if (smallBusiness) return AccrualResult.ManualInputRequired
        val grants =
            when (basis) {
                LeaveBasis.ANNIVERSARY -> monthly(hireDate) + anniversary(hireDate, until)
                LeaveBasis.FISCAL_YEAR -> monthly(hireDate) + fiscal(hireDate, until)
            }
        return AccrualResult.Calculated(
            grants.filter {
                !it.grantedOn.isAfter(until)
            }.sortedBy { it.grantedOn },
        )
    }

    // 근속 n년차 연차: 15 + (n-1)/2, 최대 25
    fun annualDays(serviceYears: Int): Int = minOf(
        MAX_DAYS,
        BASE_DAYS + maxOf(0, serviceYears - 1) / 2,
    )

    // 1년 미만 매월 1일. 1주년에 소멸
    private fun monthly(hire: LocalDate): List<LeaveGrant> {
        val firstAnniversary = hire.plusYears(1)
        return (1..MONTHLY_MAX).map {
            LeaveGrant(hire.plusMonths(it.toLong()), 1.0, firstAnniversary, monthly = true)
        }
    }

    // 주년 = plusYears (2/29 입사는 평년 2/28)
    private fun anniversary(hire: LocalDate, until: LocalDate): List<LeaveGrant> =
        generateSequence(1) { it + 1 }
            .map { n ->
                LeaveGrant(
                    hire.plusYears(n.toLong()),
                    annualDays(n).toDouble(),
                    hire.plusYears(n + 1L),
                    monthly = false,
                )
            }
            .takeWhile { !it.grantedOn.isAfter(until) }
            .toList()

    // 입사 다음 해 1/1 비례 부여(0.5 올림), 이후 1/1마다 만근속 연수 기준
    private fun fiscal(hire: LocalDate, until: LocalDate): List<LeaveGrant> {
        val firstJan = LocalDate.of(hire.year + 1, 1, 1)
        val prorated =
            LeaveGrant(firstJan, proratedDays(hire), firstJan.plusYears(1), monthly = false)
        val later =
            generateSequence(firstJan.plusYears(1)) { it.plusYears(1) }
                .takeWhile { !it.isAfter(until) }
                .map { jan ->
                    val service = maxOf(1, ChronoUnit.YEARS.between(hire, jan).toInt())
                    LeaveGrant(
                        jan,
                        annualDays(service).toDouble(),
                        jan.plusYears(1),
                        monthly = false,
                    )
                }.toList()
        return listOf(prorated) + later
    }

    // 15 × 재직일수 / 연도 일수 → 0.5일 단위 올림
    fun proratedDays(hire: LocalDate): Double {
        val yearLength = hire.lengthOfYear()
        val worked = yearLength - hire.dayOfYear + 1
        val halves = Math.floorDiv(2 * BASE_DAYS * worked + yearLength - 1, yearLength)
        return halves / 2.0
    }
}
