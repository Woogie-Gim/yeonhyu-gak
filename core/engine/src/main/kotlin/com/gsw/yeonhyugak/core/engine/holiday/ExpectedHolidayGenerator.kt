package com.gsw.yeonhyugak.core.engine.holiday

import com.gsw.yeonhyugak.core.engine.holiday.SubstituteHolidayRule.Base
import java.time.LocalDate
import java.time.MonthDay

// 미발표 연도 공휴일 계산. 선거일·임시공휴일은 계산하지 않는다
object ExpectedHolidayGenerator {
    // 이름은 특일정보 API 표기와 같게 둔다
    private val FIXED: List<Triple<MonthDay, String, SubstituteGroup?>> =
        listOf(
            Triple(MonthDay.of(1, 1), "1월1일", null),
            Triple(MonthDay.of(3, 1), "삼일절", SubstituteGroup.NATIONAL_DAYS),
            Triple(MonthDay.of(5, 5), "어린이날", SubstituteGroup.CHILDREN),
            Triple(MonthDay.of(6, 6), "현충일", null),
            Triple(MonthDay.of(8, 15), "광복절", SubstituteGroup.NATIONAL_DAYS),
            Triple(MonthDay.of(10, 3), "개천절", SubstituteGroup.NATIONAL_DAYS),
            Triple(MonthDay.of(10, 9), "한글날", SubstituteGroup.NATIONAL_DAYS),
            Triple(MonthDay.of(12, 25), "기독탄신일", SubstituteGroup.BUDDHA_CHRISTMAS),
        )

    fun generate(year: Int, lunar: LunarDates): List<Holiday> {
        val base = baseHolidays(year, lunar)
        val subs =
            SubstituteHolidayRule.substitutes(base).map {
                Holiday(
                    it,
                    SubstituteHolidayRule.NAME,
                    HolidayKind.SUBSTITUTE,
                    false,
                    HolidaySource.EXPECTED,
                )
            }
        val originals =
            base.map {
                val kind = if (it.group == SubstituteGroup.SEOL_CHUSEOK ||
                    it.name == BUDDHA
                ) {
                    HolidayKind.LUNAR
                } else {
                    HolidayKind.NATIONAL
                }
                Holiday(it.date, it.name, kind, false, HolidaySource.EXPECTED)
            }
        return (originals + subs).sortedWith(compareBy({ it.date }, { it.name }))
    }

    internal fun baseHolidays(year: Int, lunar: LunarDates): List<Base> {
        val fixed = FIXED.map { (md, name, group) -> Base(md.atYear(year), name, group) }
        val lunarDays =
            threeDays(lunar.seollal, SEOL) +
                threeDays(lunar.chuseok, CHUSEOK) +
                Base(lunar.buddha, BUDDHA, SubstituteGroup.BUDDHA_CHRISTMAS)
        return (fixed + lunarDays).filter { it.date.year == year }
    }

    // 당일 기준 양력 앞뒤 하루. 음력 12월 말일은 직접 쓰지 않는다
    private fun threeDays(day: LocalDate, name: String): List<Base> =
        (-1L..1L).map { Base(day.plusDays(it), name, SubstituteGroup.SEOL_CHUSEOK) }

    private const val SEOL = "설날"
    private const val CHUSEOK = "추석"
    private const val BUDDHA = "부처님오신날"
}
