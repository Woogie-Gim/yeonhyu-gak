package com.gsw.yeonhyugak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// 첫 실행 여부. null = 아직 읽는 중
@HiltViewModel
class AppViewModel @Inject constructor(prefs: UserPrefsRepository) : ViewModel() {
    val onboardingDone: StateFlow<Boolean?> =
        prefs.onboardingDone.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
