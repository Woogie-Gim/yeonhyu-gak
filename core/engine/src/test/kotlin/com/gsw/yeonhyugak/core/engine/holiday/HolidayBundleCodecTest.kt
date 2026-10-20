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

        test("원격 공휴일 파싱. 모르는 필드는 무시") {
            val r =
                HolidayBundleCodec.parseRemote(
                    """
                    {"version":3,"note":"x","holidays":[
                     {"date":"2028-04-12","name":"국회의원선거일","kind":"ELECTION","memo":"?"}]}
                    """.trimIndent(),
                )
            r.version shouldBe 3
            val h = r.holidays.single()
            h.date shouldBe LocalDate.of(2028, 4, 12)
            h.kind shouldBe HolidayKind.ELECTION
            h.source shouldBe HolidaySource.REMOTE
            r.toOverrides().add shouldBe listOf(h)
        }

        test("원격 스키마 위반은 파일 전체 거부") {
            val bad =
                listOf(
                    """{"holidays":[]}""",
                    """{"version":0,"holidays":[]}""",
                    """{"version":1}""",
                    remote("""{"date":"2028-04-12","name":"a"}"""),
                    remote("""{"date":"2028-04-12","name":"a","kind":"WHAT"}"""),
                    remote("""{"date":"2028/04/12","name":"a","kind":"ELECTION"}"""),
                    remote("""{"date":"2036-01-01","name":"a","kind":"ELECTION"}"""),
                    remote("""{"date":"2028-04-12","name":"","kind":"ELECTION"}"""),
                    remote(
                        """{"date":"2028-04-12","name":"a","kind":"ELECTION"},""" +
                            """{"date":"2028-04-12","name":"a","kind":"TEMPORARY"}""",
                    ),
                )
            bad.forEach { shouldThrow<HolidayDataException> { HolidayBundleCodec.parseRemote(it) } }
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

private fun remote(holidays: String) = """{"version":1,"holidays":[$holidays]}"""

private fun year2025(holidays: String) =
    """{"schemaVersion":1,"years":{"2025":{"confirmed":true,"holidays":[$holidays]}}}"""
