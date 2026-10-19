package com.gsw.yeonhyugak.core.engine.holiday

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private fun datesNamed(year: Int, lunar: LunarDates, name: String): List<LocalDate> =
    ExpectedHolidayGenerator.generate(year, lunar).filter {
        it.name == name
    }.map { it.date }

class ExpectedHolidayGeneratorTest :
    FunSpec({
        test("음력 12월이 29일로 끝나는 해(2025): 설 연휴 = 설날 양력 앞뒤 하루 → 1/28~1/30") {
            datesNamed(2025, KnownLunarDates.Y2025, "설날") shouldContainExactly
                listOf(
                    LocalDate.of(2025, 1, 28),
                    LocalDate.of(2025, 1, 29),
                    LocalDate.of(2025, 1, 30),
                )
        }

        test("음력 12월이 30일로 끝나는 해(2024): 설 연휴 2/9~2/11") {
            datesNamed(2024, KnownLunarDates.Y2024, "설날") shouldContainExactly
                listOf(
                    LocalDate.of(2024, 2, 9),
                    LocalDate.of(2024, 2, 10),
                    LocalDate.of(2024, 2, 11),
                )
        }

        test("추석 연휴 = 추석 양력 앞뒤 하루 (2025 → 10/5~10/7)") {
            datesNamed(2025, KnownLunarDates.Y2025, "추석") shouldContainExactly
                listOf(
                    LocalDate.of(2025, 10, 5),
                    LocalDate.of(2025, 10, 6),
                    LocalDate.of(2025, 10, 7),
                )
        }

        test("고정 공휴일 8개 + 음력 7일, 모두 예상(미확정) 표시") {
            val hs = ExpectedHolidayGenerator.generate(2024, KnownLunarDates.Y2024)
            hs.count { it.kind == HolidayKind.NATIONAL } shouldBe 8
            hs.count { it.kind == HolidayKind.LUNAR } shouldBe 7
            hs.all { !it.confirmed && it.source == HolidaySource.EXPECTED } shouldBe true
        }

        test("노동절·제헌절은 2026년부터 (2025 없음 · 2026 고정 공휴일 10개)") {
            val names2025 = ExpectedHolidayGenerator.generate(2025, KnownLunarDates.Y2025).map {
                it.name
            }
            (HolidayKey.LABOR.kasiName in names2025) shouldBe false
            (HolidayKey.CONSTITUTION.kasiName in names2025) shouldBe false
            val hs = ExpectedHolidayGenerator.generate(2026, KnownLunarDates.Y2026)
            hs.count { it.kind == HolidayKind.NATIONAL } shouldBe 10
            datesNamed(2026, KnownLunarDates.Y2026, "노동절") shouldContainExactly
                listOf(LocalDate.of(2026, 5, 1))
            datesNamed(2026, KnownLunarDates.Y2026, "제헌절") shouldContainExactly
                listOf(LocalDate.of(2026, 7, 17))
        }

        test("고정 공휴일 규칙의 적용 시작 연도") {
            val since = ExpectedHolidayGenerator.FIXED.associate { it.key to it.sinceYear }
            since[HolidayKey.LABOR] shouldBe 2026
            since[HolidayKey.CONSTITUTION] shouldBe 2026
            since[HolidayKey.CHRISTMAS] shouldBe null
        }

        test("선거일·임시공휴일은 계산하지 않음") {
            ExpectedHolidayGenerator
                .generate(2025, KnownLunarDates.Y2025)
                .none {
                    it.kind == HolidayKind.ELECTION || it.kind == HolidayKind.TEMPORARY
                } shouldBe
                true
        }
    })
