package com.gsw.yeonhyugak.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 사용자 설정. 연도별 연차 총량은 Phase 3 온보딩에서 입력
@Singleton
class UserPrefsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    val allowance: Flow<Double> = store.data.map { it[ALLOWANCE] ?: DEFAULT_ALLOWANCE }

    val coachMarkShown: Flow<Boolean> = store.data.map { it[COACH_MARK] ?: false }

    suspend fun setAllowance(days: Double) {
        store.edit { it[ALLOWANCE] = days }
    }

    suspend fun markCoachMarkShown() {
        store.edit { it[COACH_MARK] = true }
    }

    companion object {
        // SPEC 2장 스테퍼 기본값
        const val DEFAULT_ALLOWANCE = 15.0
        private val ALLOWANCE = doublePreferencesKey("leave_allowance")
        private val COACH_MARK = booleanPreferencesKey("coach_mark_calendar_shown")
    }
}
