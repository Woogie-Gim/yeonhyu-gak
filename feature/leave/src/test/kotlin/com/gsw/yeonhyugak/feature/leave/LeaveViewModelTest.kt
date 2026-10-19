package com.gsw.yeonhyugak.feature.leave

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.prefs.HireInfo
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.leave.LeaveBasis
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 내 연차: 남은 개수 · 수정과 되돌리기 · 소멸 예정. 오늘 = 2026-05-06
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class LeaveViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, YhDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val prefsFile = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
    private val prefs = UserPrefsRepository(PreferenceDataStoreFactory.create { prefsFile })
    private val leaves = LeaveRepository(db)
    private val store = ViewModelStore()
    private val clock =
        Clock.fixed(MAY_6.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun vm(): LeaveViewModel = ViewModelProvider.create(
        store,
        viewModelFactory { initializer { LeaveViewModel(leaves, prefs, clock) } },
    )[LeaveViewModel::class]

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        store.clear()
        db.close()
        prefsFile.delete()
    }

    @Test
    fun remaining_subtractsThisYearOnly_withHalfDays() = runTest {
        prefs.completeOnboarding(15.0)
        leaves.mark(listOf(LocalDate.of(2026, 7, 1)), LeaveType.FULL)
        leaves.mark(listOf(LocalDate.of(2026, 7, 2)), LeaveType.AM_HALF)
        leaves.mark(listOf(LocalDate.of(2027, 1, 4)), LeaveType.FULL)
        val s = vm().state.first { it.loaded && it.records.size == 2 }
        assertEquals(1.5, s.used, 0.0)
        assertEquals(13.5, s.remaining, 0.0)
        assertEquals(LocalDate.of(2026, 7, 1), s.records.first().date)
        assertNull(s.expiresOn)
    }

    @Test
    fun remaining_canGoNegative() = runTest {
        prefs.completeOnboarding(1.0)
        leaves.mark(listOf(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)), LeaveType.FULL)
        val s = vm().state.first { it.loaded && it.used == 2.0 }
        assertEquals(-1.0, s.remaining, 0.0)
    }

    @Test
    fun saveRemaining_thenUndo() = runTest {
        prefs.completeOnboarding(15.0)
        leaves.mark(listOf(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)), LeaveType.FULL)
        val vm = vm()
        vm.state.first { it.loaded && it.used == 2.0 }

        vm.startEdit()
        vm.state.first { it.editing }
        vm.saveRemaining(10.0)
        val edited = vm.state.first { !it.editing && it.remaining == 10.0 }
        assertEquals(12.0, edited.allowance, 0.0)

        val event = vm.eventFlow.first()
        vm.undo(event)
        val restored = vm.state.first { it.allowance == 15.0 }
        assertEquals(13.0, restored.remaining, 0.0)
    }

    // 입사일 기준 2025-03-02 입사: 오늘 유효한 월차는 2026-03-02에 소멸, 15일은 2027-03-02
    @Test
    fun expiry_shownOnlyWithHireDate() = runTest {
        prefs.completeOnboarding(15.0, HireInfo(LocalDate.of(2025, 3, 2), LeaveBasis.ANNIVERSARY))
        val s = vm().state.first { it.loaded && it.expiresOn != null }
        assertEquals(LocalDate.of(2027, 3, 2), s.expiresOn)
    }

    private companion object {
        val MAY_6: LocalDate = LocalDate.of(2026, 5, 6)
    }
}
