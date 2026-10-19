package com.gsw.yeonhyugak.core.engine.holiday

import java.time.LocalDate

// 규칙 테스트용 실제 음력 공휴일 양력 날짜 (번들과 무관)
object KnownLunarDates {
    val Y2018 =
        LunarDates(LocalDate.of(2018, 2, 16), LocalDate.of(2018, 5, 22), LocalDate.of(2018, 9, 24))
    val Y2020 =
        LunarDates(LocalDate.of(2020, 1, 25), LocalDate.of(2020, 4, 30), LocalDate.of(2020, 10, 1))
    val Y2021 =
        LunarDates(LocalDate.of(2021, 2, 12), LocalDate.of(2021, 5, 19), LocalDate.of(2021, 9, 21))
    val Y2023 =
        LunarDates(LocalDate.of(2023, 1, 22), LocalDate.of(2023, 5, 27), LocalDate.of(2023, 9, 29))
    val Y2024 =
        LunarDates(LocalDate.of(2024, 2, 10), LocalDate.of(2024, 5, 15), LocalDate.of(2024, 9, 17))

    // 음력 12월이 29일로 끝나는 해. 섣달그믐 = 1/28 = 음력 12/29
    val Y2025 =
        LunarDates(LocalDate.of(2025, 1, 29), LocalDate.of(2025, 5, 5), LocalDate.of(2025, 10, 6))

    val Y2026 =
        LunarDates(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 5, 24), LocalDate.of(2026, 9, 25))
    val Y2027 =
        LunarDates(LocalDate.of(2027, 2, 7), LocalDate.of(2027, 5, 13), LocalDate.of(2027, 9, 15))
    val Y2028 =
        LunarDates(LocalDate.of(2028, 1, 27), LocalDate.of(2028, 5, 2), LocalDate.of(2028, 10, 3))
}
