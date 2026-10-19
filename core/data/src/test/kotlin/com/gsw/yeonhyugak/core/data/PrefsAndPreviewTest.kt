package com.gsw.yeonhyugak.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.data.plan.PlanPreviewStore
import com.gsw.yeonhyugak.core.data.prefs.HireInfo
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.leave.LeaveBasis
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 온보딩 플래그 · 남은 개수 수정과 되돌리기 · 미리보기 저장소
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PrefsAndPreviewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun prefs(): UserPrefsRepository {
        val file = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
        return UserPrefsRepository(PreferenceDataStoreFactory.create { file })
    }

    @Test
    fun onboarding_savesAllowanceAndFlagTogether() = runTest {
        val p = prefs()
        assertFalse(p.onboardingDone.first())
        assertNull(p.hireInfo.first())

        val hire = HireInfo(LocalDate.of(2025, 3, 2), LeaveBasis.FISCAL_YEAR)
        p.completeOnboarding(11.5, hire)

        assertTrue(p.onboardingDone.first())
        assertEquals(11.5, p.allowance.first(), 0.0)
        assertEquals(hire, p.hireInfo.first())
    }

    @Test
    fun setRemaining_addsUsedAndReturnsPrevious() = runTest {
        val p = prefs()
        p.completeOnboarding(15.0)

        // 올해 3일 지정한 상태에서 남은 개수를 10으로 고치면 총량 13
        val previous = p.setRemaining(remaining = 10.0, usedThisYear = 3.0)
        assertEquals(15.0, previous, 0.0)
        assertEquals(13.0, p.allowance.first(), 0.0)

        p.setAllowance(previous)
        assertEquals(15.0, p.allowance.first(), 0.0)
    }

    @Test
    fun previewStore_showsAndClears() {
        val store = PlanPreviewStore()
        store.show(emptyList())
        assertNull(store.preview.value)

        val dates = listOf(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 23))
        store.show(dates)
        assertEquals(LocalDate.of(2026, 9, 23), store.preview.value?.focus)
        assertEquals(dates.toSet(), store.preview.value?.leaveDates)

        store.clear()
        assertNull(store.preview.value)
    }
}
