package com.gsw.yeonhyugak.core.engine

import java.time.LocalDate

// 탐색 가능 연도 범위 (SPEC 3장)
object SupportedYears {
    const val FIRST = 2020
    const val LAST = 2035

    val range: IntRange = FIRST..LAST

    fun contains(date: LocalDate): Boolean = date.year in range
}
