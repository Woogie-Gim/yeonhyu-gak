package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsw.yeonhyugak.core.data.nav.OpenDateStore
import com.gsw.yeonhyugak.core.data.prefs.AlertPrefsRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 달력 바깥과 잇는 일: 위젯·알림에서 연 날짜 · 첫 담기 후 알림 동의
class CalendarLinks internal constructor(
    private val openDates: OpenDateStore,
    private val alertPrefs: AlertPrefsRepository,
    private val scope: CoroutineScope,
) {
    // 그 달로 이동한 뒤 consumeOpenDate
    val openDate: StateFlow<LocalDate?> = openDates.date

    fun consumeOpenDate() = openDates.consume()

    // 다시 묻지 않는다. 설정에서 바꿀 수 있다
    fun answerNotify(on: Boolean) {
        scope.launch { alertPrefs.setNotify(on) }
    }

    internal suspend fun shouldAskNotify(): Boolean = !alertPrefs.notifyAsked.first()
}

// 위젯·알림에서 열면 그 날짜가 있는 달로
@Composable
internal fun OpenDateEffect(links: CalendarLinks, goTo: (YearMonth) -> Unit) {
    val openDate by links.openDate.collectAsStateWithLifecycle()
    LaunchedEffect(openDate) {
        openDate?.let {
            goTo(YearMonth.from(it))
            links.consumeOpenDate()
        }
    }
}
