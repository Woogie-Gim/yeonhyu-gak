package com.gsw.yeonhyugak.core.engine.holiday

import java.time.LocalDate

// 음력 공휴일의 양력 날짜. 설·추석 당일만 갖고 앞뒤 하루는 양력으로 더한다
data class LunarDates(val seollal: LocalDate, val buddha: LocalDate, val chuseok: LocalDate)

class LunarTable(private val years: Map<Int, LunarDates>) {
    val coveredYears: Set<Int> get() = years.keys

    operator fun get(year: Int): LunarDates? = years[year]
}
