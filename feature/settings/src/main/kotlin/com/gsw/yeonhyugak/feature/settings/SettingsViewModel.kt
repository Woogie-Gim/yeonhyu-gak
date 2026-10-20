package com.gsw.yeonhyugak.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.prefs.AlertPrefsRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.SoundMode
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.engine.calendar.MonthGrid
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val today: LocalDate,
    val appearance: Appearance = Appearance(),
    val holidays: HolidayCalendar? = null,
    val leaves: Map<LocalDate, LeaveType> = emptyMap(),
    val shareQr: Boolean = true,
    val notifyOn: Boolean = false,
) {
    // 테마 카드 미리보기: 내 실제 달력
    fun grid(month: YearMonth): MonthGrid? = holidays?.let {
        MonthGrid.build(month, it, WorkCalendar(holidays = it.holidayDates), leaves)
    }
}

sealed interface SettingsEvent {
    // 테마를 바꿨어요 · 되돌리기
    data class ThemeChanged(val theme: ThemeId, val previous: ThemeId) : SettingsEvent

    data class CheerChanged(val cheer: CheerColor, val previous: CheerColor) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    private val alertPrefs: AlertPrefsRepository,
    holidayRepo: HolidayRepository,
    leaveRepo: LeaveRepository,
    clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val events = Channel<SettingsEvent>(Channel.BUFFERED)

    val eventFlow: Flow<SettingsEvent> = events.receiveAsFlow()

    val state: StateFlow<SettingsUiState> =
        combine(
            prefs.appearance,
            holidayRepo.observe(),
            leaveRepo.observe(),
            prefs.shareQr,
            alertPrefs.notifyOn,
        ) { appearance, holidays, leaves, qr, notify ->
            SettingsUiState(today, appearance, holidays, leaves, qr, notify)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), SettingsUiState(today))

    // 탭 즉시 적용. 같은 테마면 아무 일 없음
    fun selectTheme(theme: ThemeId) = viewModelScope.launch {
        val previous = prefs.setTheme(theme)
        if (previous != theme) events.send(SettingsEvent.ThemeChanged(theme, previous))
    }

    fun selectCheer(cheer: CheerColor) = viewModelScope.launch {
        val previous = prefs.setCheer(cheer)
        if (previous != cheer) events.send(SettingsEvent.CheerChanged(cheer, previous))
    }

    fun undoTheme(previous: ThemeId) = viewModelScope.launch { prefs.setTheme(previous) }

    fun undoCheer(previous: CheerColor) = viewModelScope.launch { prefs.setCheer(previous) }

    fun setSound(mode: SoundMode) = viewModelScope.launch { prefs.setSound(mode) }

    fun setHaptic(on: Boolean) = viewModelScope.launch { prefs.setHaptic(on) }

    fun setShareQr(on: Boolean) = viewModelScope.launch { prefs.setShareQr(on) }

    // 켜기는 시스템 권한을 받은 뒤에만 호출된다
    fun setNotify(on: Boolean) = viewModelScope.launch { alertPrefs.setNotify(on) }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
