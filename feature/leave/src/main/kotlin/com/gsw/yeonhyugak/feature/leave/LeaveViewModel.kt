package com.gsw.yeonhyugak.feature.leave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.prefs.HireInfo
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.leave.LeaveAccrualCalculator
import com.gsw.yeonhyugak.core.engine.leave.LeaveLedger
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaveRecord(val date: LocalDate, val type: LeaveType)

data class LeaveUiState(
    val today: LocalDate,
    val loaded: Boolean = false,
    val allowance: Double = UserPrefsRepository.DEFAULT_ALLOWANCE,
    // 올해 지정한 연차 합. 반차 0.5
    val used: Double = 0.0,
    val records: List<LeaveRecord> = emptyList(),
    // 입사일을 넣었을 때만
    val expiresOn: LocalDate? = null,
    val editing: Boolean = false,
) {
    // 초과하면 음수 그대로 (SPEC 6장)
    val remaining: Double get() = allowance - used
}

sealed interface LeaveEvent {
    // 남은 개수를 고쳤어요 · 되돌리기
    data class RemainingEdited(val previousAllowance: Double) : LeaveEvent
}

@HiltViewModel
class LeaveViewModel @Inject constructor(
    leaveRepo: LeaveRepository,
    private val prefs: UserPrefsRepository,
    clock: Clock,
) : ViewModel() {
    private val today = LocalDate.now(clock)
    private val editing = MutableStateFlow(false)
    private val events = Channel<LeaveEvent>(Channel.BUFFERED)

    val eventFlow: Flow<LeaveEvent> = events.receiveAsFlow()

    val state: StateFlow<LeaveUiState> =
        combine(leaveRepo.observe(), prefs.allowance, prefs.hireInfo, editing) {
                leaves,
                allowance,
                hire,
                isEditing,
            ->
            LeaveUiState(
                today = today,
                loaded = true,
                allowance = allowance,
                used = LeaveLedger.used(leaves, today.year),
                records = leaves.filterKeys { it.year == today.year }
                    .map { (d, t) -> LeaveRecord(d, t) }
                    .sortedBy { it.date },
                expiresOn = hire?.let(::expiryOf),
                editing = isEditing,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), LeaveUiState(today))

    fun startEdit() = editing.update { true }

    fun cancelEdit() = editing.update { false }

    fun saveRemaining(remaining: Double) {
        editing.value = false
        viewModelScope.launch {
            val previous = prefs.setRemaining(remaining, state.value.used)
            events.send(LeaveEvent.RemainingEdited(previous))
        }
    }

    fun undo(event: LeaveEvent) = viewModelScope.launch {
        when (event) {
            is LeaveEvent.RemainingEdited -> prefs.setAllowance(event.previousAllowance)
        }
    }

    // 오늘 유효한 연차 중 가장 먼저 사라지는 날
    private fun expiryOf(hire: HireInfo): LocalDate? =
        LeaveAccrualCalculator.nextExpiry(hire.hireDate, hire.basis, today)

    private companion object {
        const val STOP_MS = 5_000L
    }
}
