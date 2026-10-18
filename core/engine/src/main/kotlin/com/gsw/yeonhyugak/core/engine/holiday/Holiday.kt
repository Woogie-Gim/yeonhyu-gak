package com.gsw.yeonhyugak.core.engine.holiday

import java.time.LocalDate

enum class HolidayKind { NATIONAL, LUNAR, SUBSTITUTE, TEMPORARY, ELECTION }

// 확정(번들) · 예상(계산) · 원격 덮어쓰기
enum class HolidaySource { BUNDLED, EXPECTED, REMOTE }

data class Holiday(
    val date: LocalDate,
    val name: String,
    val kind: HolidayKind,
    val confirmed: Boolean,
    val source: HolidaySource,
)
