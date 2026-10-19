package com.gsw.yeonhyugak.core.engine.planner

import com.gsw.yeonhyugak.core.engine.SupportedYears
import com.gsw.yeonhyugak.core.engine.calendar.VacationSpan
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.time.LocalDate

// 추천 화면 진입점. 달력에 지정한 연차를 반영하고 일수는 VacationSpan으로 센다
// work에는 bookedLeave를 넣지 않는다 (MonthGrid와 같은 입력)
class Recommender(private val planner: VacationPlanner) {
    fun today(): LocalDate = planner.today()

    fun yearOf(period: RecommendPeriod): Int = when (period) {
        RecommendPeriod.REST_OF_YEAR -> today().year
        RecommendPeriod.NEXT_YEAR -> today().year + 1
    }

    fun suggestsNextYear(): Boolean = planner.suggestsNextYear()

    // 이미 지정한 만큼 뺀 남은 연차
    fun budget(period: RecommendPeriod, allowance: Double, leaves: Map<LocalDate, LeaveType>) =
        LeaveBudget.afterUsed(yearOf(period), allowance, leaves)

    fun recommend(
        period: RecommendPeriod,
        work: WorkCalendar,
        leaves: Map<LocalDate, LeaveType>,
        allowance: Double,
        filter: CandidateFilter = CandidateFilter.NONE,
        sort: SortOrder = SortOrder.EFFICIENCY,
    ): List<Recommendation> {
        val year = yearOf(period)
        val budget = budget(period, allowance, leaves)
        val booked = work.withBooked(leaves)
        // 다음 해 연차만 쓰지만 이 기간에 시작하는 블록도 포함
        return (year..year + 1)
            .flatMap { planner.candidates(it, booked, budget) }
            .distinctBy { it.leaveDates }
            .mapNotNull { toRecommendation(it, work, leaves) }
            .filter { inPeriod(it, period) && filter.accepts(it) }
            .sortedWith(sort.comparator())
    }

    // 겹치지 않게 효율 순 탐욕 배치. 저장하지 않는 제안
    fun autoPlan(
        period: RecommendPeriod,
        work: WorkCalendar,
        leaves: Map<LocalDate, LeaveType>,
        allowance: Double,
    ): List<Recommendation> = greedyPlan(
        recommend(period, work, leaves, allowance),
        budget(period, allowance, leaves),
        { it.costByYear },
    ) { a, b -> a.overlaps(b) }.sortedBy { it.start }

    // 남은 연차 0일 때: 연차 없이 3일 이상 쉬는 구간
    fun freeBreaks(
        period: RecommendPeriod,
        work: WorkCalendar,
        leaves: Map<LocalDate, LeaveType>,
    ): List<Recommendation> {
        val booked = work.withBooked(leaves)
        return planner.freeBreaks(yearOf(period), booked, VacationSpan.MIN_DAYS)
            .mapNotNull { toRecommendation(it, work, leaves) }
            .filter { inPeriod(it, period) }
            .sortedBy { it.start }
    }

    // 일수는 VacationSpan으로 다시 센다. 지원 범위 밖 · 3일 미만은 버린다
    private fun toRecommendation(
        block: VacationBlock,
        work: WorkCalendar,
        leaves: Map<LocalDate, LeaveType>,
    ): Recommendation? = block.leaveDates
        .takeIf { it.all(SupportedYears::contains) }
        ?.let { VacationSpan.around(block.start, work, leaves, it.toSet()) }
        ?.takeIf { VacationSpan.length(it) >= VacationSpan.MIN_DAYS }
        ?.let { span ->
            val bookedInside = leaves.count { (d, t) -> t == LeaveType.FULL && d in span }
            Recommendation(block.leaveDates, span, bookedInside)
        }

    // 올해 남은 기간: 오늘 이전 날짜가 들어간 연휴는 뺀다
    private fun inPeriod(r: Recommendation, period: RecommendPeriod): Boolean {
        val year = yearOf(period)
        return r.start.year == year &&
            (period != RecommendPeriod.REST_OF_YEAR || !r.start.isBefore(today()))
    }

    // 반차는 출근일이라 연휴를 잇지 않는다
    private fun WorkCalendar.withBooked(leaves: Map<LocalDate, LeaveType>): WorkCalendar =
        copy(bookedLeave = bookedLeave + leaves.filterValues { it == LeaveType.FULL }.keys)
}
