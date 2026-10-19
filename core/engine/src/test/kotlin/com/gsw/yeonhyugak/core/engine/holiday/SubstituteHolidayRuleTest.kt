package com.gsw.yeonhyugak.core.engine.holiday

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private fun substitutesOf(year: Int, lunar: LunarDates): List<LocalDate> =
    ExpectedHolidayGenerator.generate(year, lunar).filter {
        it.kind ==
            HolidayKind.SUBSTITUTE
    }.map { it.date }

class SubstituteHolidayRuleTest :
    FunSpec({
        test("설 연휴가 일요일과 겹치면 연휴 다음 평일 (2020 설 1/24~26 → 1/27)") {
            substitutesOf(2020, KnownLunarDates.Y2020) shouldContainExactly
                listOf(LocalDate.of(2020, 1, 27))
        }

        test("추석 연휴가 일요일과 겹치면 대체 (2018 추석 9/23~25 → 9/26)") {
            substitutesOf(2018, KnownLunarDates.Y2018) shouldContainExactly
                listOf(LocalDate.of(2018, 5, 7), LocalDate.of(2018, 9, 26))
        }

        test("어린이날이 토요일이면 다음 월요일 (2018-05-05 → 05-07)") {
            substitutesOf(2018, KnownLunarDates.Y2018) shouldContainExactly
                listOf(LocalDate.of(2018, 5, 7), LocalDate.of(2018, 9, 26))
        }

        test("국경일 확대 이전에는 토요일 개천절도 대체 없음 (2020)") {
            substitutesOf(2020, KnownLunarDates.Y2020).none { it.monthValue == 10 } shouldBe true
        }

        test("국경일 확대 이후 광복절·개천절·한글날 대체 (2021), 성탄절은 아직 대상 아님") {
            substitutesOf(2021, KnownLunarDates.Y2021) shouldContainExactly
                listOf(
                    LocalDate.of(2021, 8, 16),
                    LocalDate.of(2021, 10, 4),
                    LocalDate.of(2021, 10, 11),
                )
        }

        test("부처님오신날 토요일 → 월요일 (2023-05-27 → 05-29), 설 일요일 → 1/24") {
            substitutesOf(2023, KnownLunarDates.Y2023) shouldContainExactly
                listOf(LocalDate.of(2023, 1, 24), LocalDate.of(2023, 5, 29))
        }

        test("공휴일끼리 겹침은 하나만 대체 (2025 어린이날·부처님오신날 → 5/6), 삼일절 토 → 3/3, 추석 일 → 10/8") {
            substitutesOf(2025, KnownLunarDates.Y2025) shouldContainExactly
                listOf(
                    LocalDate.of(2025, 3, 3),
                    LocalDate.of(2025, 5, 6),
                    LocalDate.of(2025, 10, 8),
                )
        }

        test("어린이날 일요일 + 설 일요일 (2024 → 2/12 · 5/6)") {
            substitutesOf(2024, KnownLunarDates.Y2024) shouldContainExactly
                listOf(LocalDate.of(2024, 2, 12), LocalDate.of(2024, 5, 6))
        }

        test("노동절·제헌절·성탄절 토요일 → 월요일 (2027 → 5/3 · 7/19 · 12/27)") {
            substitutesOf(2027, KnownLunarDates.Y2027) shouldContainExactly
                listOf(
                    LocalDate.of(2027, 2, 9),
                    LocalDate.of(2027, 5, 3),
                    LocalDate.of(2027, 7, 19),
                    LocalDate.of(2027, 8, 16),
                    LocalDate.of(2027, 10, 4),
                    LocalDate.of(2027, 10, 11),
                    LocalDate.of(2027, 12, 27),
                )
        }

        test("2026 삼일절·부처님오신날·광복절·개천절 대체, 노동절·제헌절 금요일은 대체 없음") {
            substitutesOf(2026, KnownLunarDates.Y2026) shouldContainExactly
                listOf(
                    LocalDate.of(2026, 3, 2),
                    LocalDate.of(2026, 5, 25),
                    LocalDate.of(2026, 8, 17),
                    LocalDate.of(2026, 10, 5),
                )
        }

        test("추석과 개천절 겹침은 하나만 대체 (2028-10-03 → 10/5)") {
            substitutesOf(2028, KnownLunarDates.Y2028) shouldContainExactly
                listOf(LocalDate.of(2028, 10, 5))
        }
    })
