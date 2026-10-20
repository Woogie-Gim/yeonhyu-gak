package com.gsw.yeonhyugak.core.engine

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

// 알림·위젯 예약 기준 시간대. 기기 시간대와 무관
object SeoulTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Seoul")

    fun today(clock: Clock): LocalDate = LocalDate.now(clock.withZone(ZONE))

    // 다음 at(서울)까지 남은 시간. 정확히 그 시각이면 하루 뒤
    fun delayUntil(clock: Clock, at: LocalTime): Duration {
        val now = ZonedDateTime.now(clock.withZone(ZONE))
        var next = now.toLocalDate().atTime(at).atZone(ZONE)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
