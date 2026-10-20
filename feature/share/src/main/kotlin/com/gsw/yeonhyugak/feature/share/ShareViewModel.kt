package com.gsw.yeonhyugak.feature.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.calendar.YearBreakCalculator
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

// 연차 기록 · 공휴일 · QR 설정만 읽는다. 메모·입사일 저장소는 받지 않는다
@HiltViewModel
class ShareViewModel @Inject constructor(
    holidayRepo: HolidayRepository,
    leaveRepo: LeaveRepository,
    prefs: UserPrefsRepository,
) : ViewModel() {
    private val year = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val card: StateFlow<ShareCardData?> =
        year.filterNotNull()
            .flatMapLatest { y ->
                combine(holidayRepo.observe(), leaveRepo.observe(), prefs.shareQr) {
                        cal,
                        leaves,
                        qr,
                    ->
                    val work = WorkCalendar(holidays = cal.holidayDates)
                    val breaks = YearBreakCalculator.of(y, work, leaves).highlights()
                    ShareCardData(
                        year = y,
                        blocks = breaks.spans.map { span ->
                            val named = cal.within(span.start..span.end)
                                .sortedBy { it.kind == HolidayKind.SUBSTITUTE }
                                .firstOrNull()
                            ShareBlock(span.start, span.end, span.days, named)
                        },
                        totalDays = breaks.totalDays,
                        usedLeave = breaks.usedLeave,
                        expected = cal.isExpected(y),
                        showQr = qr,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    fun show(year: Int) {
        this.year.value = year
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
