package com.gsw.yeonhyugak.core.engine

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.localDate
import io.kotest.property.checkAll
import java.time.LocalDate

// 테스트 파이프라인 확인용. Phase 1에서 엔진 테스트로 대체
class EngineSetupTest :
    FunSpec({
        test("범위 양 끝 연도는 포함, 바깥은 제외") {
            SupportedYears.contains(LocalDate.of(2020, 1, 1)) shouldBe true
            SupportedYears.contains(LocalDate.of(2035, 12, 31)) shouldBe true
            SupportedYears.contains(LocalDate.of(2019, 12, 31)) shouldBe false
            SupportedYears.contains(LocalDate.of(2036, 1, 1)) shouldBe false
        }

        test("kotest-property 1,000회: 범위 안 날짜는 항상 포함") {
            var count = 0
            checkAll(
                1_000,
                Arb.localDate(
                    LocalDate.of(SupportedYears.FIRST, 1, 1),
                    LocalDate.of(SupportedYears.LAST, 12, 31),
                ),
            ) { date ->
                SupportedYears.contains(date) shouldBe true
                count++
            }
            count shouldBe 1_000
        }
    })
