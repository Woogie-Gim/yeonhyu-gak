package com.gsw.yeonhyugak.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.holiday.NoHolidayOverrides
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.memo.MemoRepository
import com.gsw.yeonhyugak.core.data.plan.PlanPreviewStore
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.engine.planner.Recommender
import com.gsw.yeonhyugak.core.engine.planner.VacationPlanner
import com.gsw.yeonhyugak.feature.calendar.CalendarRoute
import com.gsw.yeonhyugak.feature.calendar.CalendarViewModel
import com.gsw.yeonhyugak.feature.leave.LeaveRoute
import com.gsw.yeonhyugak.feature.leave.LeaveViewModel
import com.gsw.yeonhyugak.feature.recommend.RecommendRoute
import com.gsw.yeonhyugak.feature.recommend.RecommendViewModel
import com.gsw.yeonhyugak.feature.recommend.onboarding.OnboardingRoute
import com.gsw.yeonhyugak.feature.recommend.onboarding.OnboardingViewModel
import com.gsw.yeonhyugak.feature.settings.SettingsRoute
import com.gsw.yeonhyugak.feature.settings.SettingsViewModel
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// 앱 전체 하네스: 실제 번들 공휴일 + 메모리 DB + 임시 DataStore + 고정 Clock
class AppHarness(val today: LocalDate = MAY_6) {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, YhDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val prefsFile = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
    private val store = ViewModelStore()
    val clock: Clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    val leaves = LeaveRepository(db)
    val memos = MemoRepository(db, clock)
    val prefs = UserPrefsRepository(PreferenceDataStoreFactory.create { prefsFile })
    val preview = PlanPreviewStore()
    private val holidays = HolidayRepository(context, NoHolidayOverrides(), Dispatchers.Unconfined)
    private val recommender = Recommender(VacationPlanner(clock))

    private val provider = ViewModelProvider.create(
        store,
        viewModelFactory {
            initializer { CalendarViewModel(holidays, leaves, memos, prefs, preview, clock) }
            initializer { RecommendViewModel(holidays, leaves, prefs, preview, recommender) }
            initializer { LeaveViewModel(leaves, prefs, clock) }
            initializer { OnboardingViewModel(prefs, clock) }
            initializer { AppViewModel(prefs, clock) }
            initializer { SettingsViewModel(prefs, holidays, leaves, clock) }
        },
    )

    private val screens = YhScreens(
        onboarding = {
            OnboardingRoute(onDone = it, viewModel = provider[OnboardingViewModel::class])
        },
        calendar = { host, openSettings ->
            CalendarRoute(
                snackbarHostState = host,
                onOpenSettings = openSettings,
                viewModel = provider[CalendarViewModel::class],
            )
        },
        recommend = {
            RecommendRoute(onOpenCalendar = it, viewModel = provider[RecommendViewModel::class])
        },
        leave = { host, openRecommend ->
            LeaveRoute(
                snackbarHostState = host,
                onOpenRecommend = openRecommend,
                viewModel = provider[LeaveViewModel::class],
            )
        },
        settings = { host, back ->
            SettingsRoute(
                snackbarHostState = host,
                onBack = back,
                viewModel = provider[SettingsViewModel::class],
            )
        },
    )

    // MainActivity와 같은 구성 (설정에서 읽은 테마 포함)
    @Composable
    fun App(dark: Boolean = false) {
        AppRoot(provider[AppViewModel::class], screens, darkTheme = dark)
    }

    fun appearance(): Appearance = runBlocking { prefs.appearance.first() }

    fun setTheme(theme: ThemeId, cheer: CheerColor? = null) = runBlocking {
        prefs.setTheme(theme)
        cheer?.let { prefs.setCheer(it) }
    }

    // 온보딩을 이미 끝낸 사용자
    fun onboarded(allowance: Double = 15.0, coachShown: Boolean = true) = runBlocking {
        prefs.completeOnboarding(allowance)
        if (coachShown) prefs.markCoachMarkShown()
    }

    fun mark(vararg dates: LocalDate, type: LeaveType = LeaveType.FULL) = runBlocking {
        leaves.mark(dates.toList(), type)
    }

    fun recommendState() = provider[RecommendViewModel::class].state.value

    fun leaveMap(): Map<LocalDate, LeaveType> = runBlocking { leaves.observe().first() }

    fun onboardingDone(): Boolean = runBlocking { prefs.onboardingDone.first() }

    fun allowance(): Double = runBlocking { prefs.allowance.first() }

    fun close() {
        store.clear()
        db.close()
        prefsFile.delete()
    }

    companion object {
        val MAY_6: LocalDate = LocalDate.of(2026, 5, 6)
        val NOV_2: LocalDate = LocalDate.of(2026, 11, 2)
    }
}
