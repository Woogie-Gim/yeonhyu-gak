package com.gsw.yeonhyugak.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.SoundMode
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 테마 저장 · 다시 열어도 유지 · 깨진 값 · 되돌리기 값
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AppearancePrefsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")

    // 같은 파일을 새 DataStore로 연다 (앱 재시작과 같음)
    private fun open(scope: CoroutineScope) =
        PreferenceDataStoreFactory.create(scope = scope) { file }

    @Test
    fun defaults_noteSystemSoundHapticOn() = runTest {
        val scope = CoroutineScope(SupervisorJob())
        assertEquals(Appearance(), UserPrefsRepository(open(scope)).appearance.first())
        scope.cancel()
    }

    @Test
    fun saved_survivesReopen() = runTest {
        val first = CoroutineScope(SupervisorJob())
        val p = UserPrefsRepository(open(first))
        p.setTheme(ThemeId.LOFI)
        p.setCheer(CheerColor.NAVY)
        p.setSound(SoundMode.OFF)
        p.setHaptic(false)
        first.cancel()

        val second = CoroutineScope(SupervisorJob())
        val reopened = UserPrefsRepository(open(second)).appearance.first()
        assertEquals(Appearance(ThemeId.LOFI, CheerColor.NAVY, SoundMode.OFF, false), reopened)
        second.cancel()
    }

    @Test
    fun setTheme_returnsPreviousForUndo() = runTest {
        val scope = CoroutineScope(SupervisorJob())
        val p = UserPrefsRepository(open(scope))
        assertEquals(ThemeId.NOTE, p.setTheme(ThemeId.BASEBALL))
        assertEquals(ThemeId.BASEBALL, p.setTheme(ThemeId.NOTE))
        assertEquals(CheerColor.RED, p.setCheer(CheerColor.GREEN))
        assertEquals(ThemeId.NOTE, p.appearance.first().theme)
        scope.cancel()
    }

    @Test
    fun brokenValue_fallsBackToDefault() = runTest {
        val scope = CoroutineScope(SupervisorJob())
        val store = open(scope)
        store.edit {
            it[stringPreferencesKey("theme_id")] = "GALAXY"
            it[stringPreferencesKey("sound_mode")] = ""
        }
        val a = UserPrefsRepository(store).appearance.first()
        assertEquals(ThemeId.NOTE, a.theme)
        assertEquals(SoundMode.SYSTEM, a.sound)
        scope.cancel()
    }
}
