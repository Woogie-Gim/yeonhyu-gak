package com.gsw.yeonhyugak.core.engine

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class SupportedYearsTest :
    FunSpec({
        test("범위 양 끝 연도는 포함, 바깥은 제외") {
            SupportedYears.contains(LocalDate.of(2020, 1, 1)) shouldBe true
            SupportedYears.contains(LocalDate.of(2035, 12, 31)) shouldBe true
            SupportedYears.contains(LocalDate.of(2019, 12, 31)) shouldBe false
            SupportedYears.contains(LocalDate.of(2036, 1, 1)) shouldBe false
        }
    })
