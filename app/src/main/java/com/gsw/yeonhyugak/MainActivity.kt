package com.gsw.yeonhyugak

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.gsw.yeonhyugak.core.data.nav.OpenDateStore
import com.gsw.yeonhyugak.core.engine.SupportedYears
import com.gsw.yeonhyugak.core.ui.LocalSfxPlayer
import com.gsw.yeonhyugak.dev.DevMenus
import com.gsw.yeonhyugak.feature.calendar.CalendarRoute
import com.gsw.yeonhyugak.feature.leave.LeaveRoute
import com.gsw.yeonhyugak.feature.recommend.RecommendRoute
import com.gsw.yeonhyugak.feature.recommend.onboarding.OnboardingRoute
import com.gsw.yeonhyugak.feature.settings.SettingsRoute
import com.gsw.yeonhyugak.feature.share.ShareRoute
import com.gsw.yeonhyugak.ui.AppRoot
import com.gsw.yeonhyugak.ui.AppViewModel
import com.gsw.yeonhyugak.ui.YhScreens
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

private val Screens = YhScreens(
    onboarding = { OnboardingRoute(onDone = it) },
    calendar = { host, openSettings, openShare ->
        CalendarRoute(
            snackbarHostState = host,
            onOpenSettings = openSettings,
            onOpenShare = openShare,
        )
    },
    recommend = { RecommendRoute(onOpenCalendar = it) },
    leave = { host, openRecommend ->
        LeaveRoute(snackbarHostState = host, onOpenRecommend = openRecommend)
    },
    settings = { host, back, devMenu ->
        SettingsRoute(snackbarHostState = host, onBack = back, devMenu = devMenu)
    },
    share = { year, dismiss -> ShareRoute(year = year, onDismiss = dismiss) },
    devMenu = DevMenus.entry,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    @Inject
    lateinit var openDates: OpenDateStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) openFrom(intent)
        val player = (application as YhApplication).sfxPlayer
        setContent {
            CompositionLocalProvider(LocalSfxPlayer provides player) {
                AppRoot(appViewModel, Screens)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFrom(intent)
    }

    // 위젯·알림 탭: 그 날짜가 있는 달로 연다
    private fun openFrom(intent: Intent?) {
        val day = intent?.getLongExtra(OpenDateStore.EXTRA_OPEN_DATE, NO_DATE) ?: return
        if (day == NO_DATE) return
        val date = LocalDate.ofEpochDay(day)
        if (SupportedYears.contains(date)) openDates.open(date)
        intent.removeExtra(OpenDateStore.EXTRA_OPEN_DATE)
    }

    private companion object {
        const val NO_DATE = Long.MIN_VALUE
    }
}
