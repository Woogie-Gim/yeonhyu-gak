package com.gsw.yeonhyugak.core.ui

// 일 수 표시: 정수면 3, 반차 섞이면 2.5
object DayCount {
    fun format(days: Double): String =
        if (days % 1.0 == 0.0) days.toLong().toString() else days.toString()
}
