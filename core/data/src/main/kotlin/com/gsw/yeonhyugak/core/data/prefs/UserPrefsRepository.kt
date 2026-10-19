package com.gsw.yeonhyugak.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gsw.yeonhyugak.core.engine.leave.LeaveBasis
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 입사일 경로로 계산했을 때만 저장. 소멸 예정 표시용
data class HireInfo(val hireDate: LocalDate, val basis: LeaveBasis)

// 사용자 설정. allowance = 올해 연차 총량. 남은 개수 = 총량 − 달력에 지정한 연차
@Singleton
class UserPrefsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    val allowance: Flow<Double> = store.data.map { it[ALLOWANCE] ?: DEFAULT_ALLOWANCE }

    val coachMarkShown: Flow<Boolean> = store.data.map { it[COACH_MARK] ?: false }

    val onboardingDone: Flow<Boolean> = store.data.map { it[ONBOARDING] ?: false }

    val hireInfo: Flow<HireInfo?> = store.data.map { p ->
        val day = p[HIRE_DATE] ?: return@map null
        val basis = p[BASIS]?.let { runCatching { LeaveBasis.valueOf(it) }.getOrNull() }
        HireInfo(LocalDate.ofEpochDay(day), basis ?: LeaveBasis.ANNIVERSARY)
    }

    suspend fun setAllowance(days: Double) {
        store.edit { it[ALLOWANCE] = days }
    }

    // 온보딩 끝: 첫 실행이라 지정한 연차가 없으므로 남은 개수 = 총량. 한 번에 저장
    suspend fun completeOnboarding(remaining: Double, hire: HireInfo? = null) {
        store.edit {
            it[ALLOWANCE] = remaining
            it[ONBOARDING] = true
            if (hire != null) {
                it[HIRE_DATE] = hire.hireDate.toEpochDay()
                it[BASIS] = hire.basis.name
            }
        }
    }

    // 내 연차 화면에서 남은 개수 수정. 되돌리기용으로 이전 총량을 돌려준다
    suspend fun setRemaining(remaining: Double, usedThisYear: Double): Double {
        var previous = DEFAULT_ALLOWANCE
        store.edit {
            previous = it[ALLOWANCE] ?: DEFAULT_ALLOWANCE
            it[ALLOWANCE] = remaining + usedThisYear
        }
        return previous
    }

    suspend fun markCoachMarkShown() {
        store.edit { it[COACH_MARK] = true }
    }

    companion object {
        // SPEC 2장 스테퍼 기본값
        const val DEFAULT_ALLOWANCE = 15.0
        private val ALLOWANCE = doublePreferencesKey("leave_allowance")
        private val COACH_MARK = booleanPreferencesKey("coach_mark_calendar_shown")
        private val ONBOARDING = booleanPreferencesKey("onboarding_done")
        private val HIRE_DATE = longPreferencesKey("hire_date_epoch_day")
        private val BASIS = stringPreferencesKey("leave_basis")
    }
}
