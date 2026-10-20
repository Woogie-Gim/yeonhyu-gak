package com.gsw.yeonhyugak.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 알림 동의 · 보낸 알림 기록 · 원격으로 새로 생긴 공휴일. 설정과 같은 DataStore
@Singleton
class AlertPrefsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    // 첫 담기 직후 한 번만 묻는다
    val notifyAsked: Flow<Boolean> = store.data.map { it[NOTIFY_ASKED] ?: false }

    val notifyOn: Flow<Boolean> = store.data.map { it[NOTIFY_ON] ?: false }

    suspend fun setNotify(on: Boolean) {
        store.edit {
            it[NOTIFY_ASKED] = true
            it[NOTIFY_ON] = on
        }
    }

    // 같은 알림 중복 방지
    val alertSentKeys: Flow<Set<String>> = store.data.map { it[ALERT_SENT].orEmpty() }

    suspend fun addAlertSent(keys: Collection<String>) {
        if (keys.isEmpty()) return
        store.edit { it[ALERT_SENT] = it[ALERT_SENT].orEmpty() + keys }
    }

    // 감지 후 NEW_BADGE_DAYS일까지 N 배지
    fun newHolidays(today: LocalDate): Flow<Set<LocalDate>> = store.data.map { p ->
        p[NEW_HOLIDAYS].orEmpty()
            .mapNotNull(::parseNewDay)
            .filter { (_, seen) -> fresh(seen, today) }
            .map { it.first }
            .toSet()
    }

    suspend fun addNewHolidays(dates: Collection<LocalDate>, today: LocalDate) {
        store.edit { p ->
            val kept = p[NEW_HOLIDAYS].orEmpty().filter { e ->
                parseNewDay(e)?.let { fresh(it.second, today) } == true
            }
            p[NEW_HOLIDAYS] =
                kept.toSet() + dates.map { "${it.toEpochDay()}:${today.toEpochDay()}" }
        }
    }

    private fun fresh(seen: LocalDate, today: LocalDate): Boolean =
        !today.isAfter(seen.plusDays(NEW_BADGE_DAYS))

    // "날짜:감지일" epochDay 쌍. 깨진 값은 버린다
    private fun parseNewDay(entry: String): Pair<LocalDate, LocalDate>? {
        val parts = entry.split(':').mapNotNull(String::toLongOrNull)
        return if (parts.size == 2) {
            LocalDate.ofEpochDay(parts[0]) to LocalDate.ofEpochDay(parts[1])
        } else {
            null
        }
    }

    companion object {
        const val NEW_BADGE_DAYS = 14L
        private val NOTIFY_ASKED = booleanPreferencesKey("notify_asked")
        private val NOTIFY_ON = booleanPreferencesKey("notify_on")
        private val ALERT_SENT = stringSetPreferencesKey("alert_sent_keys")
        private val NEW_HOLIDAYS = stringSetPreferencesKey("remote_new_days")
    }
}
