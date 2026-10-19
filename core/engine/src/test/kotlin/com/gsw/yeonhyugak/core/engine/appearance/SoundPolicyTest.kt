package com.gsw.yeonhyugak.core.engine.appearance

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SoundPolicyTest :
    FunSpec({
        test("무음·진동 모드면 모든 설정에서 재생 안 함") {
            listOf(RingerState.SILENT, RingerState.VIBRATE).forEach { ringer ->
                SoundMode.entries.forEach { mode ->
                    listOf(true, false).forEach { touch ->
                        SoundPolicy.shouldPlay(mode, ringer, touch) shouldBe false
                    }
                }
            }
        }

        test("벨소리 모드: 끄기는 안 함 · 켜기는 재생 · 시스템은 터치음 설정 따름") {
            SoundPolicy.shouldPlay(SoundMode.OFF, RingerState.NORMAL, true) shouldBe false
            SoundPolicy.shouldPlay(SoundMode.ON, RingerState.NORMAL, false) shouldBe true
            SoundPolicy.shouldPlay(SoundMode.SYSTEM, RingerState.NORMAL, true) shouldBe true
            SoundPolicy.shouldPlay(SoundMode.SYSTEM, RingerState.NORMAL, false) shouldBe false
        }

        test("야구 비시즌은 11~2월") {
            (1..12).filterNot(BaseballSeason::isOn) shouldBe listOf(1, 2, 11, 12)
        }
    })
