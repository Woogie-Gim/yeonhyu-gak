package com.gsw.yeonhyugak.core.engine.holiday

import com.gsw.yeonhyugak.core.engine.holiday.SubstituteHolidayRule.Base
import java.time.LocalDate
import java.time.MonthDay

// 고정 공휴일 규칙. sinceYear 이전 연도는 공휴일 아님 (null = 지원 범위 전체)
data class FixedHolidayRule(
    val key: HolidayKey,
    val monthDay: MonthDay,
    val sinceYear: Int?,
    val group: SubstituteGroup?,
) {
    fun appliesTo(year: Int): Boolean = sinceYear == null || year >= sinceYear
}

// 미발표 연도 공휴일 계산. 선거일·임시공휴일은 계산하지 않는다
object ExpectedHolidayGenerator {
    val FIXED: List<FixedHolidayRule> =
        listOf(
            FixedHolidayRule(HolidayKey.NEW_YEAR, MonthDay.of(1, 1), null, null),
            FixedHolidayRule(
                HolidayKey.INDEPENDENCE,
                MonthDay.of(3, 1),
                null,
                SubstituteGroup.NATIONAL_DAYS,
            ),
            // 2026 공휴일 지정 (KASI 특일정보 기준)
            FixedHolidayRule(HolidayKey.LABOR, MonthDay.of(5, 1), 2026, SubstituteGroup.LABOR_DAY),
            FixedHolidayRule(
                HolidayKey.CHILDREN,
                MonthDay.of(5, 5),
                null,
                SubstituteGroup.CHILDREN,
            ),
            FixedHolidayRule(HolidayKey.MEMORIAL, MonthDay.of(6, 6), null, null),
            // 2026 공휴일 재지정 (KASI 특일정보 기준)
            FixedHolidayRule(
                HolidayKey.CONSTITUTION,
                MonthDay.of(7, 17),
                2026,
                SubstituteGroup.NATIONAL_DAYS,
            ),
            FixedHolidayRule(
                HolidayKey.LIBERATION,
                MonthDay.of(8, 15),
                null,
                SubstituteGroup.NATIONAL_DAYS,
            ),
            FixedHolidayRule(
                HolidayKey.FOUNDATION,
                MonthDay.of(10, 3),
                null,
                SubstituteGroup.NATIONAL_DAYS,
            ),
            FixedHolidayRule(
                HolidayKey.HANGUL,
                MonthDay.of(10, 9),
                null,
                SubstituteGroup.NATIONAL_DAYS,
            ),
            FixedHolidayRule(
                HolidayKey.CHRISTMAS,
                MonthDay.of(12, 25),
                null,
                SubstituteGroup.BUDDHA_CHRISTMAS,
            ),
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
        val fixed =
            FIXED.filter { it.appliesTo(year) }.map {
                Base(it.monthDay.atYear(year), it.key.kasiName, it.group)
            }
        val lunarDays =
            threeDays(lunar.seollal, SEOL) +
                threeDays(lunar.chuseok, CHUSEOK) +
                Base(lunar.buddha, BUDDHA, SubstituteGroup.BUDDHA_CHRISTMAS)
        return (fixed + lunarDays).filter { it.date.year == year }
    }

    // 당일 기준 양력 앞뒤 하루. 음력 12월 말일은 직접 쓰지 않는다
    private fun threeDays(day: LocalDate, name: String): List<Base> =
        (-1L..1L).map { Base(day.plusDays(it), name, SubstituteGroup.SEOL_CHUSEOK) }

    private val SEOL = HolidayKey.SEOLLAL.kasiName
    private val CHUSEOK = HolidayKey.CHUSEOK.kasiName
    private val BUDDHA = HolidayKey.BUDDHA.kasiName
}
