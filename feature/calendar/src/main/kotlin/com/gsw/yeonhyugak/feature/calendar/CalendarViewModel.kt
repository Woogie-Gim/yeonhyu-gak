package com.gsw.yeonhyugak.feature.calendar

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.memo.MemoRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.calendar.LeaveToggle
import com.gsw.yeonhyugak.core.engine.calendar.MonthGrid
import com.gsw.yeonhyugak.core.engine.calendar.RangeSelection
import com.gsw.yeonhyugak.core.engine.calendar.RangeSummary
import com.gsw.yeonhyugak.core.engine.calendar.TapResult
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayCalendar
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiMessage(@param:StringRes val res: Int, val args: List<Any> = emptyList())

sealed interface CalendarEvent {
    // 스낵바. undoable이면 되돌리기 버튼
    data class Changed(val message: UiMessage, val undoable: Boolean = true) : CalendarEvent

    // 말풍선 안내
    data class HolidayTip(val holidays: List<Holiday>) : CalendarEvent

    data object WeekendTip : CalendarEvent

    data class AnchorTip(val date: LocalDate) : CalendarEvent
}

// 시트에 띄운 날짜. 메모 내용은 시트에서만 쓴다
data class DaySheetState(val date: LocalDate, val memo: String?)

data class CalendarUiState(
    val today: LocalDate,
    val holidays: HolidayCalendar? = null,
    val leaves: Map<LocalDate, LeaveType> = emptyMap(),
    // 달력에는 메모 유무만
    val memoDates: Set<LocalDate> = emptySet(),
    val allowance: Double = UserPrefsRepository.DEFAULT_ALLOWANCE,
    val coachMarkVisible: Boolean = false,
    val rangeAnchor: LocalDate? = null,
    val sheet: DaySheetState? = null,
) {
    val isLoaded: Boolean get() = holidays != null

    // 회사 휴무일·근무 요일 설정은 Phase 6
    val work: WorkCalendar get() = WorkCalendar(holidays = holidays?.holidayDates.orEmpty())

    fun grid(month: YearMonth): MonthGrid? =
        holidays?.let { MonthGrid.build(month, it, work, leaves) }

    fun summarize(from: LocalDate, to: LocalDate): RangeSummary =
        RangeSelection.summarize(from, to, work, leaves, allowance)

    fun isExpected(year: Int): Boolean = holidays?.isExpected(year) ?: false

    fun isWorkday(date: LocalDate): Boolean = work.isWorkday(date)
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val holidayRepo: HolidayRepository,
    leaveRepo: LeaveRepository,
    private val memoRepo: MemoRepository,
    private val prefs: UserPrefsRepository,
    clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val local = MutableStateFlow(LocalState())
    private val events = Channel<CalendarEvent>(Channel.BUFFERED)
    private val editor = CalendarEditor(leaveRepo, memoRepo)

    val eventFlow: Flow<CalendarEvent> = events.receiveAsFlow()

    val state: StateFlow<CalendarUiState> =
        combine(
            flow { emit(holidayRepo.calendar()) },
            leaveRepo.observe(),
            memoRepo.observeDates(),
            combine(prefs.allowance, prefs.coachMarkShown, ::Pair),
            local,
        ) { holidays, leaves, memoDates, (allowance, coachShown), l ->
            CalendarUiState(
                today = today,
                holidays = holidays,
                leaves = leaves,
                memoDates = memoDates,
                allowance = allowance,
                coachMarkVisible = !coachShown,
                rangeAnchor = l.anchor,
                sheet = l.sheet,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), CalendarUiState(today))

    fun onTap(date: LocalDate) = launch {
        val s = state.value
        when (LeaveToggle.onTap(date, s.work, s.leaves)) {
            is TapResult.Marked -> {
                editor.mark(listOf(date), LeaveType.FULL)
                changed(doneMessage(LeaveType.FULL), marked = true)
            }

            is TapResult.Cleared -> {
                editor.clear(date)
                changed(doneMessage(null))
            }

            is TapResult.NotWorkday -> {
                val holidays = s.holidays?.on(date).orEmpty()
                events.send(
                    if (holidays.isEmpty()) {
                        CalendarEvent.WeekendTip
                    } else {
                        CalendarEvent.HolidayTip(
                            holidays,
                        )
                    },
                )
            }
        }
    }

    fun commitRange(from: LocalDate, to: LocalDate) = launch {
        local.update { it.copy(anchor = null) }
        val summary = state.value.summarize(from, to)
        if (summary.leaveDates.isEmpty()) {
            events.send(
                CalendarEvent.Changed(UiMessage(R.string.cal_done_range_none), undoable = false),
            )
        } else {
            editor.mark(summary.leaveDates, LeaveType.FULL)
            changed(
                UiMessage(R.string.cal_done_range, listOf(DayCount.format(summary.cost))),
                marked = true,
            )
        }
    }

    // null = 지정 해제
    fun setLeave(date: LocalDate, type: LeaveType?) = launch {
        closeSheet()
        if (type == null) editor.clear(date) else editor.mark(listOf(date), type)
        changed(doneMessage(type), marked = type != null)
    }

    fun openSheet(date: LocalDate) = launch {
        local.update { it.copy(sheet = DaySheetState(date, memoRepo.text(date))) }
    }

    fun closeSheet() = local.update { it.copy(sheet = null) }

    // 빈 글이면 삭제
    fun saveMemo(date: LocalDate, text: String) = launch {
        closeSheet()
        editor.saveMemo(date, text)
        changed(
            UiMessage(
                if (text.isBlank()) R.string.cal_done_memo_delete else R.string.cal_done_memo_save,
            ),
        )
    }

    // TalkBack 범위 선택: 시작점 기억
    fun rangeFrom(date: LocalDate) = launch {
        closeSheet()
        local.update { it.copy(anchor = date) }
        events.send(CalendarEvent.AnchorTip(date))
    }

    fun rangeTo(date: LocalDate) {
        val anchor = local.value.anchor ?: return
        closeSheet()
        commitRange(anchor, date)
    }

    fun dismissCoachMark() = launch { prefs.markCoachMarkShown() }

    fun undo() = launch { editor.undo() }

    // 첫 지정에 성공하면 코치마크를 닫고 다시 띄우지 않는다
    private suspend fun changed(message: UiMessage, marked: Boolean = false) {
        if (marked && state.value.coachMarkVisible) prefs.markCoachMarkShown()
        events.send(CalendarEvent.Changed(message))
    }

    private data class LocalState(val anchor: LocalDate? = null, val sheet: DaySheetState? = null)

    private companion object {
        const val STOP_MS = 5_000L
    }
}

private fun ViewModel.launch(block: suspend () -> Unit) {
    viewModelScope.launch { block() }
}

private fun doneMessage(type: LeaveType?): UiMessage = UiMessage(
    when (type) {
        LeaveType.FULL -> R.string.cal_done_full
        LeaveType.AM_HALF -> R.string.cal_done_am
        LeaveType.PM_HALF -> R.string.cal_done_pm
        null -> R.string.cal_done_clear
    },
)
