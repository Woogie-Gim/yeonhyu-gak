package com.gsw.yeonhyugak.feature.recommend

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
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.holiday.NoHolidayOverrides
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.plan.PlanPreviewStore
import com.gsw.yeonhyugak.core.data.prefs.HireInfo
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.leave.AccrualResult
import com.gsw.yeonhyugak.core.engine.leave.LeaveAccrualCalculator
import com.gsw.yeonhyugak.core.engine.leave.LeaveBasis
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.RecommendPeriod
import com.gsw.yeonhyugak.core.engine.planner.Recommender
import com.gsw.yeonhyugak.core.engine.planner.Season
import com.gsw.yeonhyugak.core.engine.planner.VacationPlanner
import com.gsw.yeonhyugak.feature.recommend.onboarding.OnboardingViewModel
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 추천 ViewModel · 온보딩 입사일 경로. 오늘 = 2026-05-06
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RecommendViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, YhDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val prefsFile = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
    private val prefs = UserPrefsRepository(PreferenceDataStoreFactory.create { prefsFile })
    private val leaves = LeaveRepository(db)
    private val preview = PlanPreviewStore()
    private val holidays = HolidayRepository(context, NoHolidayOverrides(), Dispatchers.Unconfined)
    private val store = ViewModelStore()

    private fun clockAt(date: LocalDate): Clock =
        Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun recommendVm(today: LocalDate = MAY_6): RecommendViewModel =
        ViewModelProvider.create(
            store,
            viewModelFactory {
                initializer {
                    RecommendViewModel(
                        holidays,
                        leaves,
                        prefs,
                        preview,
                        Recommender(VacationPlanner(clockAt(today))),
                    )
                }
            },
        )[RecommendViewModel::class]

    private fun onboardingVm(): OnboardingViewModel = ViewModelProvider.create(
        store,
        viewModelFactory { initializer { OnboardingViewModel(prefs, clockAt(MAY_6)) } },
    )[OnboardingViewModel::class]

    // stateIn(WhileSubscribed)이라 구독하면서 조건을 기다린다
    private suspend fun RecommendViewModel.awaitState(
        predicate: (RecommendUiState) -> Boolean,
    ): RecommendUiState = state.first(predicate)

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
    fun default_isRestOfYear_andSubtractsUsed() = runTest {
        prefs.completeOnboarding(5.0)
        leaves.mark(listOf(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)), LeaveType.FULL)
        val s = recommendVm().awaitState { it.isLoaded && it.items.isNotEmpty() }
        assertEquals(RecommendPeriod.REST_OF_YEAR, s.choice.period)
        assertEquals(3.0, s.remaining, 0.0)
        assertFalse(s.nextYearHint)
        s.items.forEach {
            assertFalse(it.start.isBefore(MAY_6))
            assertTrue((it.costByYear[2026] ?: 0) <= 3)
        }
    }

    @Test
    fun preview_sendsCardDatesToStore() = runTest {
        prefs.completeOnboarding(15.0)
        val vm = recommendVm()
        val first = vm.awaitState { it.items.isNotEmpty() }.items.first()
        vm.preview(first)
        assertEquals(first.leaveDates.toSet(), preview.preview.value?.leaveDates)
        assertEquals(first.leaveDates.min(), preview.preview.value?.focus)
        // 담기 전에는 저장하지 않는다
        assertTrue(leaves.observe().first().isEmpty())
    }

    @Test
    fun autoPlan_previewsWithinBudget() = runTest {
        prefs.completeOnboarding(6.0)
        val vm = recommendVm()
        vm.awaitState { it.items.isNotEmpty() }
        assertTrue(vm.previewAutoPlan())
        val dates = preview.preview.value!!.leaveDates
        assertTrue(dates.count { it.year == 2026 } <= 6)
        assertTrue(leaves.observe().first().isEmpty())
    }

    @Test
    fun filters_toggleAndClear() = runTest {
        prefs.completeOnboarding(15.0)
        val vm = recommendVm()
        vm.awaitState { it.items.isNotEmpty() }
        vm.toggleSeason(Season.AUTUMN)
        vm.toggleMinLength(5)
        val filtered = vm.awaitState {
            it.choice.season == Season.AUTUMN && it.choice.minLength == 5
        }
        filtered.items.forEach {
            assertTrue(it.start.month in Season.AUTUMN.months)
            assertTrue(it.totalDays >= 5)
        }
        vm.clearFilters()
        val cleared = vm.awaitState { it.choice.season == null && it.choice.minLength == null }
        assertTrue(cleared.items.size >= filtered.items.size)
    }

    @Test
    fun november_highlightsNextYear() = runTest {
        prefs.completeOnboarding(15.0)
        val s = recommendVm(LocalDate.of(2026, 11, 2)).awaitState { it.isLoaded }
        assertTrue(s.nextYearHint)
    }

    @Test
    fun zeroLeft_showsFreeBreaksOnly() = runTest {
        prefs.completeOnboarding(0.0)
        val s = recommendVm().awaitState { it.isLoaded && it.freeBreaks.isNotEmpty() }
        assertTrue(s.noLeaveLeft)
        assertTrue(s.items.isEmpty())
        s.freeBreaks.forEach { assertEquals(0, it.cost) }
    }

    // 입사일 경로 값 = 엔진 계산값
    @Test
    fun onboarding_hirePath_equalsEngine() = runTest {
        val vm = onboardingVm()
        val hire = LocalDate.of(2023, 3, 2)
        listOf(LeaveBasis.ANNIVERSARY, LeaveBasis.FISCAL_YEAR).forEach { basis ->
            vm.openHire()
            vm.setHireDate(hire)
            vm.setBasis(basis)
            val expected =
                (LeaveAccrualCalculator.calculate(hire, MAY_6, basis) as AccrualResult.Calculated)
                    .entitlementOn(MAY_6)
            assertEquals(expected, vm.state.value.calculated!!, 0.0)
            vm.applyHire()
            assertEquals(expected, vm.state.value.remaining, 0.0)
            assertFalse(vm.state.value.hireOpen)
        }

        val done = CompletableDeferred<Unit>()
        vm.complete { done.complete(Unit) }
        done.await()
        assertTrue(prefs.onboardingDone.first())
        assertEquals(HireInfo(hire, LeaveBasis.FISCAL_YEAR), prefs.hireInfo.first())
    }

    @Test
    fun onboarding_manualEditDropsHireInfo() = runTest {
        val vm = onboardingVm()
        vm.setHireDate(LocalDate.of(2023, 3, 2))
        vm.applyHire()
        vm.setRemaining(10.0)
        assertNull(vm.state.value.appliedHire)
        // 미래 입사일은 오늘로
        vm.setHireDate(LocalDate.of(2030, 1, 1))
        assertEquals(MAY_6, vm.state.value.hireDate)
    }

    private companion object {
        val MAY_6: LocalDate = LocalDate.of(2026, 5, 6)
    }
}
