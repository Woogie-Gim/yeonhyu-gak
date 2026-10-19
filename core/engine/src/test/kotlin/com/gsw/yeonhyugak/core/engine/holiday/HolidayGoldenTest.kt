package com.gsw.yeonhyugak.core.engine.holiday

import com.gsw.yeonhyugak.core.engine.SupportedYears
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.File
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun asset(name: String): String =
    File(requireNotNull(System.getProperty("yh.assetsDir")), name).readText()

// holiday_sync.py --update 가 만든 KASI 원본 스냅샷 (date, name)
private fun golden(): Map<Int, List<Pair<LocalDate, String>>> {
    val text = requireNotNull(
        HolidayGoldenTest::class.java.getResource("/golden/kasi_rest_days.json"),
    ).readText()
    return Json.parseToJsonElement(text).jsonObject.map { (year, items) ->
        year.toInt() to items.jsonArray.map {
            val o = it.jsonObject
            val d = o.getValue("locdate").jsonPrimitive.int
            LocalDate.of(d / 10_000, d / 100 % 100, d % 100) to
                o.getValue("dateName").jsonPrimitive.content
        }
    }.toMap()
}

private val NOT_COMPUTED = setOf(HolidayKind.ELECTION, HolidayKind.TEMPORARY)

class HolidayGoldenTest :
    FunSpec({
        val bundle = HolidayBundleCodec.parseBundle(asset("kr_holidays.json"))
        val lunar = HolidayBundleCodec.parseLunarTable(asset("lunar_table.json"))
        val snapshot = golden()

        test("번들 확정 연도 = KASI 스냅샷") {
            bundle.confirmedYears.keys.sorted() shouldContainExactly snapshot.keys.sorted()
            snapshot.forEach { (year, expected) ->
                bundle.confirmedYears.getValue(year).map { it.date to it.name }.sorted() shouldBe
                    expected.sorted()
            }
        }

        test("2020~2028 확정 · 이후 예상") {
            bundle.confirmedYears.keys.sorted() shouldContainExactly (2020..2028).toList()
        }

        test("확정 연도마다 예상 계산 = 발표 (선거일·임시공휴일 제외, 날짜·종류)") {
            bundle.confirmedYears.forEach { (year, confirmed) ->
                val actual = confirmed.filter { it.kind !in NOT_COMPUTED }
                    .map { it.date to it.kind }.sorted()
                val expected = ExpectedHolidayGenerator.generate(year, requireNotNull(lunar[year]))
                    .map { it.date to it.kind }.sorted()
                withClue(year) { expected shouldBe actual }
            }
        }

        test("선거일·임시공휴일 외 모든 KASI 이름이 키로 해석된다") {
            bundle.confirmedYears.values.flatten()
                .filter { it.kind !in NOT_COMPUTED }
                .filter {
                    val p = it.parsedName
                    p.key == null && !(p.isSubstitute && it.kind == HolidayKind.SUBSTITUTE)
                }
                .shouldBeEmpty()
        }

        test("대체공휴일(원래 이름) 괄호 안은 모두 알려진 키") {
            bundle.confirmedYears.values.flatten()
                .filter { it.name.startsWith("${HolidayNames.SUBSTITUTE}(") }
                .filter { it.parsedName.substituteOf == null }
                .shouldBeEmpty()
        }

        test("음력 변환표가 지원 연도 전부를 덮는다") {
            lunar.coveredYears shouldBe SupportedYears.range.toSet()
        }
    })

private fun <A : Comparable<A>, B : Comparable<B>> List<Pair<A, B>>.sorted(): List<Pair<A, B>> =
    sortedWith(compareBy({ it.first }, { it.second }))
