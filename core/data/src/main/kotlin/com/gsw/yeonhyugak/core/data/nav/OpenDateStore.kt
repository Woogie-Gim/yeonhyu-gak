package com.gsw.yeonhyugak.core.data.nav

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 위젯·알림 탭으로 열 날짜. 달력이 그 달로 이동한 뒤 consume
@Singleton
class OpenDateStore @Inject constructor() {
    private val current = MutableStateFlow<LocalDate?>(null)

    val date: StateFlow<LocalDate?> = current.asStateFlow()

    fun open(date: LocalDate) {
        current.value = date
    }

    fun consume() {
        current.value = null
    }

    companion object {
        // 위젯·알림 인텐트 extra. 값 = epochDay
        const val EXTRA_OPEN_DATE = "open_date"
    }
}
