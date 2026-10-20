package com.gsw.yeonhyugak.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.SoundMode
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
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

    // 테마·효과음·햅틱. 깨진 값은 기본값으로 읽는다
    val appearance: Flow<Appearance> = store.data.map { p ->
        val default = Appearance()
        Appearance(
            theme = enumOr(p[THEME], default.theme),
            cheer = enumOr(p[CHEER], default.cheer),
            sound = enumOr(p[SOUND], default.sound),
            haptic = p[HAPTIC] ?: default.haptic,
        )
    }

    // 되돌리기용으로 이전 값을 돌려준다
    suspend fun setTheme(theme: ThemeId): ThemeId = swap(THEME, theme, Appearance().theme)

    suspend fun setCheer(cheer: CheerColor): CheerColor = swap(CHEER, cheer, Appearance().cheer)

    suspend fun setSound(mode: SoundMode) {
        store.edit { it[SOUND] = mode.name }
    }

    suspend fun setHaptic(on: Boolean) {
        store.edit { it[HAPTIC] = on }
    }

    private suspend inline fun <reified E : Enum<E>> swap(
        key: Preferences.Key<String>,
        value: E,
        default: E,
    ): E {
        var previous = default
        store.edit {
            previous = enumOr(it[key], default)
            it[key] = value.name
        }
        return previous
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

    // 공유 카드 QR. 기본 켬
    val shareQr: Flow<Boolean> = store.data.map { it[SHARE_QR] ?: true }

    suspend fun setShareQr(on: Boolean) {
        store.edit { it[SHARE_QR] = on }
    }

    companion object {
        // SPEC 2장 스테퍼 기본값
        const val DEFAULT_ALLOWANCE = 15.0
        private val ALLOWANCE = doublePreferencesKey("leave_allowance")
        private val COACH_MARK = booleanPreferencesKey("coach_mark_calendar_shown")
        private val ONBOARDING = booleanPreferencesKey("onboarding_done")
        private val HIRE_DATE = longPreferencesKey("hire_date_epoch_day")
        private val BASIS = stringPreferencesKey("leave_basis")
        private val THEME = stringPreferencesKey("theme_id")
        private val CHEER = stringPreferencesKey("theme_cheer_color")
        private val SOUND = stringPreferencesKey("sound_mode")
        private val HAPTIC = booleanPreferencesKey("haptic_on")
        private val SHARE_QR = booleanPreferencesKey("share_qr_on")
    }
}

private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
