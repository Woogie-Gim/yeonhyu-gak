package com.gsw.yeonhyugak.core.engine.alert

import com.gsw.yeonhyugak.core.engine.calendar.BreakSpan
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed interface Alert {
    // 같은 키는 한 번만 보낸다
    val key: String
    val daysLeft: Int

    // 연휴 D-21: 예약 챙기기
    data class BreakSoon(val span: BreakSpan, override val daysLeft: Int) : Alert {
        override val key: String get() = "break:${span.start}:${span.end}"
    }

    // 연차 소멸 D-30
    data class LeaveExpiring(val expiresOn: LocalDate, override val daysLeft: Int) : Alert {
        override val key: String get() = "expiry:$expiresOn"
    }
}

// 오늘 보낼 알림. 작업이 며칠 늦게 돌아도 LATE_DAYS 안이면 한 번 보낸다
object AlertPlanner {
    const val BREAK_LEAD_DAYS = 21
    const val EXPIRY_LEAD_DAYS = 30
    const val LATE_DAYS = 7

    fun due(
        today: LocalDate,
        planned: List<BreakSpan>,
        expiry: LocalDate?,
        sentKeys: Set<String>,
    ): List<Alert> {
        val breaks = planned.mapNotNull { span ->
            left(today, span.start, BREAK_LEAD_DAYS)?.let { Alert.BreakSoon(span, it) }
        }
        val expiring = expiry?.let { e ->
            left(today, e, EXPIRY_LEAD_DAYS)?.let { Alert.LeaveExpiring(e, it) }
        }
        return (breaks + listOfNotNull(expiring)).filter { it.key !in sentKeys }
    }

    // lead일 전 ~ lead-LATE_DAYS일 전 사이면 남은 일수
    private fun left(today: LocalDate, target: LocalDate, lead: Int): Int? {
        val days = ChronoUnit.DAYS.between(today, target).toInt()
        return days.takeIf { it in (lead - LATE_DAYS)..lead }
    }
}
