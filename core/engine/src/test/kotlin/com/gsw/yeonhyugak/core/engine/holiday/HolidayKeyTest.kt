package com.gsw.yeonhyugak.core.engine.holiday

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HolidayKeyTest :
    FunSpec({
        test("KASI 이름으로 키 찾기") {
            HolidayKey.fromKasiName("기독탄신일") shouldBe HolidayKey.CHRISTMAS
            HolidayKey.fromKasiName("노동절") shouldBe HolidayKey.LABOR
            HolidayKey.fromKasiName("제헌절") shouldBe HolidayKey.CONSTITUTION
            HolidayKey.fromKasiName("전국동시지방선거") shouldBe null
        }

        test("대체공휴일 이름 해석") {
            HolidayNames.parse("대체공휴일") shouldBe HolidayName(null, true, null)
            HolidayNames.parse("대체공휴일(기독탄신일)") shouldBe
                HolidayName(null, true, HolidayKey.CHRISTMAS)
            HolidayNames.parse("설날") shouldBe HolidayName(HolidayKey.SEOLLAL, false, null)
            HolidayNames.parse("임시공휴일") shouldBe HolidayName(null, false, null)
        }

        test("KASI 이름은 서로 다르다") {
            HolidayKey.entries.map { it.kasiName }.toSet().size shouldBe HolidayKey.entries.size
        }
    })
