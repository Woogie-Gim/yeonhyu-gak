package com.gsw.yeonhyugak.core.engine.holiday

import com.gsw.yeonhyugak.core.engine.SupportedYears
import java.time.LocalDate

// 확정 → 예상 → 원격 순으로 병합한 공휴일 달력
class HolidayCalendar(
    bundle: HolidayBundle,
    lunar: LunarTable,
    overrides: HolidayOverrides = HolidayOverrides.NONE,
    years: IntRange = SupportedYears.range,
) {
    private val expectedYears: Set<Int> = years.filterNot { bundle.isConfirmed(it) }.toSet()

    private val byYear: Map<Int, List<Holiday>> =
        years.associateWith { year ->
            val base =
                bundle.confirmedYears[year]
                    ?: lunar[year]?.let { ExpectedHolidayGenerator.generate(year, it) }
                    ?: throw HolidayDataException("$year 음력 변환표가 없어요")
            val removed = base.filterNot { (it.date to it.name) in overrides.remove }
            val added = overrides.add.filter {
                it.date.year == year &&
                    removed.none { h -> h.date == it.date && h.name == it.name }
            }
            (removed + added).sortedWith(compareBy({ it.date }, { it.name }))
        }

    private val byDate: Map<LocalDate, List<Holiday>> = byYear.values.flatten().groupBy { it.date }

    val holidayDates: Set<LocalDate> get() = byDate.keys

    fun holidaysOf(year: Int): List<Holiday> = byYear[year].orEmpty()

    fun on(date: LocalDate): List<Holiday> = byDate[date].orEmpty()

    // 구간 안 공휴일. 추천 카드 연휴 이름용
    fun within(range: ClosedRange<LocalDate>): List<Holiday> =
        range.start.datesUntil(range.endInclusive.plusDays(1)).toList().flatMap(::on)

    fun isHoliday(date: LocalDate): Boolean = date in byDate

    // 예상 배지 대상 연도
    fun isExpected(year: Int): Boolean = year in expectedYears
}
