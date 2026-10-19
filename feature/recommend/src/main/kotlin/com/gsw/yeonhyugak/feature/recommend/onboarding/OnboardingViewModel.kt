package com.gsw.yeonhyugak.feature.recommend.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsw.yeonhyugak.core.data.prefs.HireInfo
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.leave.AccrualResult
import com.gsw.yeonhyugak.core.engine.leave.LeaveAccrualCalculator
import com.gsw.yeonhyugak.core.engine.leave.LeaveBasis
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val today: LocalDate,
    val remaining: Double = UserPrefsRepository.DEFAULT_ALLOWANCE,
    val hireOpen: Boolean = false,
    val hireDate: LocalDate? = null,
    val basis: LeaveBasis = LeaveBasis.ANNIVERSARY,
    // 입사일 경로로 채웠을 때만 저장
    val appliedHire: HireInfo? = null,
) {
    // 올해 생기는 연차. 엔진 계산 그대로
    val calculated: Double?
        get() = hireDate?.let {
            when (val r = LeaveAccrualCalculator.calculate(it, today, basis)) {
                is AccrualResult.Calculated -> r.entitlementOn(today)
                AccrualResult.ManualInputRequired -> null
            }
        }
}

// 첫 실행 질문 하나: 남은 연차. 끝나면 바로 추천
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPrefsRepository,
    clock: Clock,
) : ViewModel() {
    private val local = MutableStateFlow(OnboardingUiState(LocalDate.now(clock)))
    val state: StateFlow<OnboardingUiState> = local.asStateFlow()

    // 직접 고치면 입사일 계산값과 달라지므로 소멸 예정 계산에 쓰지 않는다
    fun setRemaining(days: Double) = local.update { it.copy(remaining = days, appliedHire = null) }

    fun openHire() = local.update { it.copy(hireOpen = true) }

    fun closeHire() = local.update { it.copy(hireOpen = false) }

    // 미래 입사일은 오늘로
    fun setHireDate(date: LocalDate) = local.update { it.copy(hireDate = minOf(date, it.today)) }

    fun setBasis(basis: LeaveBasis) = local.update { it.copy(basis = basis) }

    fun applyHire() = local.update { s ->
        val value = s.calculated ?: return@update s
        val hire = s.hireDate ?: return@update s
        s.copy(remaining = value, hireOpen = false, appliedHire = HireInfo(hire, s.basis))
    }

    fun complete(onDone: () -> Unit) {
        val s = local.value
        viewModelScope.launch {
            prefs.completeOnboarding(s.remaining, s.appliedHire)
            onDone()
        }
    }
}
