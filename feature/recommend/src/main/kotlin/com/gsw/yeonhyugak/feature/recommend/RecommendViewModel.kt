package com.gsw.yeonhyugak.feature.recommend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.plan.PlanPreviewStore
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.holiday.HolidayCalendar
import com.gsw.yeonhyugak.core.engine.leave.LeaveLedger
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.CandidateFilter
import com.gsw.yeonhyugak.core.engine.planner.RecommendPeriod
import com.gsw.yeonhyugak.core.engine.planner.Recommendation
import com.gsw.yeonhyugak.core.engine.planner.Recommender
import com.gsw.yeonhyugak.core.engine.planner.Season
import com.gsw.yeonhyugak.core.engine.planner.SortOrder
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.Month
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

// 사용자가 고른 칩·정렬
data class RecommendChoice(
    val period: RecommendPeriod = RecommendPeriod.REST_OF_YEAR,
    val season: Season? = null,
    val month: Month? = null,
    val minLength: Int? = null,
    val sort: SortOrder = SortOrder.EFFICIENCY,
) {
    val filter: CandidateFilter get() = CandidateFilter(
        season = season,
        month = month,
        minLength = minLength,
    )
}

data class RecommendUiState(
    val choice: RecommendChoice = RecommendChoice(),
    val nextYearHint: Boolean = false,
    val holidays: HolidayCalendar? = null,
    // 기간 연도의 남은 연차 (총량 − 지정분)
    val remaining: Double = 0.0,
    val year: Int = 0,
    val items: List<Recommendation> = emptyList(),
    // 남은 연차가 1일 미만일 때 연차 없이 쉬는 날
    val freeBreaks: List<Recommendation> = emptyList(),
) {
    val isLoaded: Boolean get() = holidays != null

    val noLeaveLeft: Boolean get() = remaining < 1.0
}

@HiltViewModel
class RecommendViewModel @Inject constructor(
    holidayRepo: HolidayRepository,
    leaveRepo: LeaveRepository,
    prefs: UserPrefsRepository,
    private val previewStore: PlanPreviewStore,
    private val recommender: Recommender,
) : ViewModel() {
    private val choice = MutableStateFlow(RecommendChoice())

    @Volatile
    private var latest: Inputs? = null

    val state: StateFlow<RecommendUiState> =
        combine(
            flow { emit(holidayRepo.calendar()) },
            leaveRepo.observe(),
            prefs.allowance,
            choice,
        ) { holidays, leaves, allowance, c ->
            val inputs = Inputs(holidays, leaves, allowance).also { latest = it }
            build(inputs, c)
        }.flowOn(Dispatchers.Default).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_MS),
            RecommendUiState(nextYearHint = recommender.suggestsNextYear()),
        )

    fun selectPeriod(period: RecommendPeriod) = choice.update { it.copy(period = period) }

    // 같은 칩을 다시 누르면 해제
    fun toggleSeason(season: Season) =
        choice.update { it.copy(season = season.takeIf { s -> s != it.season }, month = null) }

    fun selectMonth(month: Month?) = choice.update { it.copy(month = month, season = null) }

    fun toggleMinLength(days: Int) =
        choice.update { it.copy(minLength = days.takeIf { d -> d != it.minLength }) }

    fun selectSort(sort: SortOrder) = choice.update { it.copy(sort = sort) }

    // 기간·정렬은 두고 필터만 푼다
    fun clearFilters() = choice.update { it.copy(season = null, month = null, minLength = null) }

    // 카드 탭: 달력 미리보기로 보낸다. 저장은 달력에서 담기를 눌러야
    fun preview(item: Recommendation) = previewStore.show(item.leaveDates)

    // 연간 자동 배치 제안 전체를 미리보기로
    fun previewAutoPlan(): Boolean {
        val inputs = latest ?: return false
        val plan = recommender.autoPlan(
            choice.value.period,
            inputs.work,
            inputs.leaves,
            inputs.allowance,
        )
        previewStore.show(plan.flatMap { it.leaveDates })
        return plan.isNotEmpty()
    }

    private fun build(inputs: Inputs, c: RecommendChoice): RecommendUiState {
        val year = recommender.yearOf(c.period)
        val remaining = LeaveLedger.remaining(inputs.allowance, inputs.leaves, year)
        val items = recommender.recommend(
            c.period,
            inputs.work,
            inputs.leaves,
            inputs.allowance,
            c.filter,
            c.sort,
        )
        val breaks = if (remaining < 1.0) {
            recommender.freeBreaks(c.period, inputs.work, inputs.leaves)
        } else {
            emptyList()
        }
        return RecommendUiState(
            choice = c,
            nextYearHint = recommender.suggestsNextYear(),
            holidays = inputs.holidays,
            remaining = remaining,
            year = year,
            items = items,
            freeBreaks = breaks,
        )
    }

    private data class Inputs(
        val holidays: HolidayCalendar,
        val leaves: Map<LocalDate, LeaveType>,
        val allowance: Double,
    ) {
        // 근무 요일·회사 휴무일 설정은 Phase 6
        val work: WorkCalendar get() = WorkCalendar(holidays.holidayDates)
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
