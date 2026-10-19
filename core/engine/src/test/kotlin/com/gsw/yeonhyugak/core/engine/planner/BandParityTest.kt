package com.gsw.yeonhyugak.core.engine.planner

import com.gsw.yeonhyugak.core.engine.calendar.MonthGrid
import com.gsw.yeonhyugak.core.engine.holiday.HolidayBundleCodec
import com.gsw.yeonhyugak.core.engine.holiday.HolidayCalendar
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

private fun asset(name: String): String =
    File(requireNotNull(System.getProperty("yh.assetsDir")), name).readText()

// 추천 카드 일수 = 달력 띠 일수 (미리보기 · 담은 뒤). 번들 공휴일 그대로
class BandParityTest :
    FunSpec({
        val holidays = HolidayCalendar(
            bundle = HolidayBundleCodec.parseBundle(asset("kr_holidays.json")),
            lunar = HolidayBundleCodec.parseLunarTable(asset("lunar_table.json")),
        )
        val work = WorkCalendar(holidays.holidayDates)

        // 블록이 지나는 모든 달의 모든 칸에서 띠 길이 확인
        fun assertBand(r: Recommendation, leaves: Map<LocalDate, LeaveType>, preview: Boolean) {
            val months = r.start.datesUntil(r.end.plusDays(1)).toList()
                .map(YearMonth::from).distinct()
            months.forEach { month ->
                val grid = if (preview) {
                    MonthGrid.build(month, holidays, work, leaves, r.leaveDates.toSet())
                } else {
                    val after = leaves + r.leaveDates.associateWith { LeaveType.FULL }
                    MonthGrid.build(month, holidays, work, after)
                }
                grid.days.filter { it.date in r.span }.forEach { cell ->
                    withClue("${r.span} @ ${cell.date}") {
                        cell.band?.length shouldBe r.totalDays
                    }
                }
            }
        }

        listOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 5, 6),
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2029, 1, 1),
        ).forEach { today ->
            test("$today 기준 모든 추천: 카드 일수 = 띠 일수") {
                val rec = Recommender(VacationPlanner(fixedClock(today)))
                RecommendPeriod.entries.forEach { period ->
                    val list = rec.recommend(period, work, emptyMap(), 15.0)
                    list.shouldNotBeEmpty()
                    list.forEach {
                        assertBand(it, emptyMap(), preview = true)
                        assertBand(it, emptyMap(), preview = false)
                    }
                }
            }
        }

        test("기존 연차와 붙은 블록도 일치 (반차 섞음)") {
            val rec = Recommender(VacationPlanner(fixedClock(LocalDate.of(2026, 1, 1))))
            val leaves = mapOf(
                LocalDate.of(2026, 5, 4) to LeaveType.FULL,
                LocalDate.of(2026, 8, 14) to LeaveType.FULL,
                LocalDate.of(2026, 10, 2) to LeaveType.AM_HALF,
                LocalDate.of(2026, 12, 24) to LeaveType.FULL,
            )
            val list = rec.recommend(RecommendPeriod.REST_OF_YEAR, work, leaves, 15.0)
            list.any { it.bookedInside > 0 } shouldBe true
            list.forEach {
                assertBand(it, leaves, preview = true)
                assertBand(it, leaves, preview = false)
            }
        }

        test("연도 경계 블록은 두 달 모두 일치") {
            val rec = Recommender(VacationPlanner(fixedClock(LocalDate.of(2026, 1, 1))))
            val cross = rec.recommend(RecommendPeriod.REST_OF_YEAR, work, emptyMap(), 15.0)
                .filter { it.spansYears }
            cross.shouldNotBeEmpty()
            cross.forEach { assertBand(it, emptyMap(), preview = true) }
        }

        test("연차 없이 쉬는 날도 띠와 일치") {
            val rec = Recommender(VacationPlanner(fixedClock(LocalDate.of(2026, 1, 1))))
            val breaks = rec.freeBreaks(RecommendPeriod.REST_OF_YEAR, work, emptyMap())
            breaks.shouldNotBeEmpty()
            breaks.forEach { assertBand(it, emptyMap(), preview = true) }
        }
    })
