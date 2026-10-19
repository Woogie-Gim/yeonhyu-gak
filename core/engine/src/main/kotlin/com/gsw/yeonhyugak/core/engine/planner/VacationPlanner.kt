package com.gsw.yeonhyugak.core.engine.planner

import java.time.Clock
import java.time.LocalDate
import java.time.Month

// 추천 엔진. 현재 날짜는 주입받은 Clock에서만 읽는다
class VacationPlanner(private val clock: Clock) {
    fun today(): LocalDate = LocalDate.now(clock)

    // 올해 남은 기간 칩
    fun remainingThisYear(): CandidateFilter = CandidateFilter(from = today())

    // 11월 이후면 "내년 보기" 칩 강조
    fun suggestsNextYear(): Boolean = today().month >= NEXT_YEAR_HINT_FROM

    // 비용 1~한도의 모든 극대 블록. 계획 연도 연차가 1일 이상 들어간 블록만
    fun candidates(
        year: Int,
        calendar: WorkCalendar,
        budget: LeaveBudget,
        sort: SortOrder = SortOrder.EFFICIENCY,
        filter: CandidateFilter = CandidateFilter.NONE,
    ): List<VacationBlock> {
        if (budget.limitFor(year) == 0) return emptyList()
        val maxCost = (year - 1..year + 1).maxOf(budget::limitFor)
        val days = window(year, maxCost)
        val work = days.indices.filter { calendar.isWorkday(days[it]) }
        val result = mutableListOf<VacationBlock>()
        for (a in 1 until work.size - 1) {
            for (b in a until minOf(work.size - 1, a + maxCost)) {
                val leave = work.subList(a, b + 1).map { days[it] }
                val block = VacationBlock(days[work[a - 1] + 1], days[work[b + 1] - 1], leave)
                if (fitsBudget(block, year, budget) && filter.accepts(block)) result += block
            }
        }
        return result.sortedWith(sort.comparator())
    }

    // 겹치지 않게 효율 순 탐욕 배치. 저장하지 않는 제안
    fun autoPlan(year: Int, calendar: WorkCalendar, budget: LeaveBudget): List<VacationBlock> =
        greedyPlan(candidates(year, calendar, budget), budget, { it.costByYear }) { a, b ->
            a.overlaps(b)
        }.sortedBy { it.start }

    // 남은 연차 0일 때: 연차 없이 minLength일 이상 쉬는 구간
    fun freeBreaks(
        year: Int,
        calendar: WorkCalendar,
        minLength: Int = FREE_BREAK_MIN_DAYS,
    ): List<VacationBlock> {
        val days = window(year, 0)
        val result = mutableListOf<VacationBlock>()
        var i = 0
        while (i < days.size) {
            if (calendar.isWorkday(days[i])) {
                i++
                continue
            }
            var j = i
            while (j + 1 < days.size && !calendar.isWorkday(days[j + 1])) j++
            val block = VacationBlock(days[i], days[j], emptyList())
            // 창 가장자리에 닿은 구간은 잘렸을 수 있어 제외
            val insideWindow = i > 0 && j < days.lastIndex
            if (insideWindow && block.touchesYear(year) && block.totalDays >= minLength) {
                result += block
            }
            i = j + 1
        }
        return result
    }

    private fun VacationBlock.touchesYear(year: Int): Boolean =
        start.year == year || end.year == year

    private fun fitsBudget(block: VacationBlock, year: Int, budget: LeaveBudget): Boolean {
        val byYear = block.costByYear
        return byYear.getOrDefault(year, 0) > 0 && byYear.all { (y, c) -> c <= budget.limitFor(y) }
    }

    // 연도 경계 블록이 잘리지 않게 앞뒤로 넓힌다
    private fun window(year: Int, maxCost: Int): List<LocalDate> {
        val margin = WINDOW_MARGIN_DAYS + maxCost * 2L
        val start = LocalDate.of(year, 1, 1).minusDays(margin)
        val end = LocalDate.of(year, 12, 31).plusDays(margin)
        return start.datesUntil(end.plusDays(1)).toList()
    }

    companion object {
        const val FREE_BREAK_MIN_DAYS = 3
        private val NEXT_YEAR_HINT_FROM = Month.NOVEMBER
        private const val WINDOW_MARGIN_DAYS = 31L
    }
}

// 정렬된 후보를 앞에서부터 예산·겹침 안에서 고른다
internal fun <T> greedyPlan(
    sorted: List<T>,
    budget: LeaveBudget,
    costByYear: (T) -> Map<Int, Int>,
    overlaps: (T, T) -> Boolean,
): List<T> {
    val used = mutableMapOf<Int, Int>()
    val chosen = mutableListOf<T>()
    for (item in sorted) {
        val cost = costByYear(item)
        val fits = cost.all { (y, c) -> used.getOrDefault(y, 0) + c <= budget.limitFor(y) }
        if (fits && chosen.none { overlaps(it, item) }) {
            chosen += item
            cost.forEach { (y, c) -> used.merge(y, c, Int::plus) }
        }
    }
    return chosen
}
