package com.gsw.yeonhyugak.core.engine.holiday

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class HolidayBundleCodecTest :
    FunSpec({
        val bundle =
            """
            {"schemaVersion":1,"years":{
              "2025":{"confirmed":true,"holidays":[
                {"date":"2025-01-01","name":"1월1일","kind":"NATIONAL"},
                {"date":"2025-01-27","name":"임시공휴일","kind":"TEMPORARY"}]},
              "2030":{"confirmed":false,"holidays":[]}}}
            """.trimIndent()

        test("확정 연도만 번들 공휴일로 읽음") {
            val b = HolidayBundleCodec.parseBundle(bundle)
            b.isConfirmed(2025) shouldBe true
            b.isConfirmed(2030) shouldBe false
            b.confirmedYears.getValue(2025).map { it.kind } shouldContainExactly
                listOf(HolidayKind.NATIONAL, HolidayKind.TEMPORARY)
            b.confirmedYears.getValue(2025).all {
                it.confirmed && it.source == HolidaySource.BUNDLED
            } shouldBe
                true
        }

        test("음력표 파싱. 검증용 필드는 무시") {
            val t =
                HolidayBundleCodec.parseLunarTable(
                    """
                    {"schemaVersion":1,"years":{"2025":{"seollal":"2025-01-29",
                     "buddha":"2025-05-05","chuseok":"2025-10-06","seollalEveLunarDay":29}}}
                    """.trimIndent(),
                )
            t[2025] shouldBe KnownLunarDates.Y2025
        }

        test("원격 덮어쓰기 파싱") {
            val o =
                HolidayBundleCodec.parseOverrides(
                    """
                    {"schemaVersion":1,
                     "add":[{"date":"2028-04-12","name":"국회의원선거일","kind":"ELECTION"}],
                     "remove":[{"date":"2028-05-01","name":"x"}]}
                    """.trimIndent(),
                )
            o.add.single().source shouldBe HolidaySource.REMOTE
            o.remove shouldBe setOf(LocalDate.of(2028, 5, 1) to "x")
        }

        test("잘못된 데이터는 HolidayDataException") {
            val bad =
                listOf(
                    """not json""",
                    """{"schemaVersion":2,"years":{}}""",
                    """{"schemaVersion":1,"years":{"2019":{"confirmed":true,"holidays":[]}}}""",
                    """{"schemaVersion":1,"years":{"abc":{"confirmed":true,"holidays":[]}}}""",
                    year2025("""{"date":"2025-13-01","name":"a"}"""),
                    year2025("""{"date":"2026-01-01","name":"a"}"""),
                    year2025("""{"date":"2025-01-01","name":" "}"""),
                    year2025("""{"date":"2025-01-01","name":"a","kind":"WHAT"}"""),
                    year2025(
                        """{"date":"2025-01-01","name":"a"},{"date":"2025-01-01","name":"a"}""",
                    ),
                )
            bad.forEach { shouldThrow<HolidayDataException> { HolidayBundleCodec.parseBundle(it) } }
        }
    })

private fun year2025(holidays: String) =
    """{"schemaVersion":1,"years":{"2025":{"confirmed":true,"holidays":[$holidays]}}}"""
