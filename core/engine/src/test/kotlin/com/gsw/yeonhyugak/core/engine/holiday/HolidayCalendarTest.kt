package com.gsw.yeonhyugak.core.engine.holiday

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class HolidayCalendarTest :
    FunSpec({
        val confirmed2025 =
            listOf(
                Holiday(
                    LocalDate.of(2025, 1, 27),
                    "임시공휴일",
                    HolidayKind.TEMPORARY,
                    true,
                    HolidaySource.BUNDLED,
                ),
            )
        val bundle = HolidayBundle(mapOf(2025 to confirmed2025))
        val lunar = LunarTable(mapOf(2024 to KnownLunarDates.Y2024, 2025 to KnownLunarDates.Y2025))

        test("확정 연도는 번들 그대로, 미발표 연도는 예상 + 배지") {
            val cal = HolidayCalendar(bundle, lunar, years = 2024..2025)
            cal.holidaysOf(2025) shouldBe confirmed2025
            cal.isExpected(2025) shouldBe false
            cal.isExpected(2024) shouldBe true
            cal.holidaysOf(2024).all { !it.confirmed } shouldBe true
            cal.isHoliday(LocalDate.of(2024, 2, 12)) shouldBe true
        }

        test("원격 덮어쓰기: 추가는 REMOTE, 삭제는 제외") {
            val add =
                Holiday(
                    LocalDate.of(2024, 4, 10),
                    "국회의원선거일",
                    HolidayKind.ELECTION,
                    true,
                    HolidaySource.REMOTE,
                )
            val overrides = HolidayOverrides(listOf(add), setOf(LocalDate.of(2024, 6, 6) to "현충일"))
            val cal = HolidayCalendar(bundle, lunar, overrides, 2024..2025)
            cal.on(LocalDate.of(2024, 4, 10)) shouldBe listOf(add)
            cal.isHoliday(LocalDate.of(2024, 6, 6)) shouldBe false
            cal.isExpected(2024) shouldBe true
        }

        test("같은 날 같은 이름 원격 추가는 중복되지 않음") {
            val dup =
                Holiday(
                    LocalDate.of(2025, 1, 27),
                    "임시공휴일",
                    HolidayKind.TEMPORARY,
                    true,
                    HolidaySource.REMOTE,
                )
            HolidayCalendar(
                bundle,
                lunar,
                HolidayOverrides(listOf(dup)),
                2025..2025,
            ).on(dup.date).size shouldBe
                1
        }

        test("음력표 없는 미발표 연도는 오류") {
            shouldThrow<HolidayDataException> { HolidayCalendar(bundle, lunar, years = 2026..2026) }
        }
    })
