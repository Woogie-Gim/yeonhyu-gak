package com.gsw.yeonhyugak.core.engine

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// 기기 시간대와 무관하게 서울 기준
class SeoulTimeTest :
    FunSpec({
        // 2026-10-21 00:30 KST
        val instant = Instant.parse("2026-10-20T15:30:00Z")

        test("UTC 15:30은 서울 다음 날") {
            SeoulTime.today(Clock.fixed(instant, ZoneId.of("UTC"))) shouldBe
                LocalDate.of(2026, 10, 21)
        }

        test("기기가 뉴욕이어도 같은 지연") {
            listOf("UTC", "America/New_York", "Asia/Seoul").forEach { zone ->
                val clock = Clock.fixed(instant, ZoneId.of(zone))
                SeoulTime.delayUntil(clock, LocalTime.MIDNIGHT) shouldBe
                    Duration.ofMinutes(23 * 60 + 30)
                SeoulTime.delayUntil(clock, LocalTime.of(9, 0)) shouldBe
                    Duration.ofMinutes(8 * 60 + 30)
            }
        }

        test("정확히 그 시각이면 하루 뒤") {
            val nine = Instant.parse("2026-10-21T00:00:00Z")
            SeoulTime.delayUntil(Clock.fixed(nine, ZoneId.of("UTC")), LocalTime.of(9, 0)) shouldBe
                Duration.ofDays(1)
        }
    })
