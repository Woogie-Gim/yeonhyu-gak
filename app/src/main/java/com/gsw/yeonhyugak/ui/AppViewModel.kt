package com.gsw.yeonhyugak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsw.yeonhyugak.core.data.nav.OpenDateStore
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// 첫 실행 여부 · 테마 설정. null = 아직 읽는 중
@HiltViewModel
class AppViewModel @Inject constructor(
    prefs: UserPrefsRepository,
    clock: Clock,
    openDates: OpenDateStore,
) : ViewModel() {
    val onboardingDone: StateFlow<Boolean?> =
        prefs.onboardingDone.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // DataStore 값이 바뀌면 바로 다시 그린다 (재시작 없음)
    val appearance: StateFlow<Appearance?> =
        prefs.appearance.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // 위젯·알림에서 연 날짜. 달력 탭으로 바꾸고 이동은 달력이 한다
    val openDate: StateFlow<LocalDate?> = openDates.date

    // 앱 공통 색(계절 테마)은 이번 달 기준
    val month: Int = LocalDate.now(clock).monthValue
}
