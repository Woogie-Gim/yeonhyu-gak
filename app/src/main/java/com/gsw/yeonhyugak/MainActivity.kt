package com.gsw.yeonhyugak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.gsw.yeonhyugak.core.ui.LocalSfxPlayer
import com.gsw.yeonhyugak.feature.calendar.CalendarRoute
import com.gsw.yeonhyugak.feature.leave.LeaveRoute
import com.gsw.yeonhyugak.feature.recommend.RecommendRoute
import com.gsw.yeonhyugak.feature.recommend.onboarding.OnboardingRoute
import com.gsw.yeonhyugak.feature.settings.SettingsRoute
import com.gsw.yeonhyugak.ui.AppRoot
import com.gsw.yeonhyugak.ui.AppViewModel
import com.gsw.yeonhyugak.ui.YhScreens
import dagger.hilt.android.AndroidEntryPoint

private val Screens = YhScreens(
    onboarding = { OnboardingRoute(onDone = it) },
    calendar = { host, openSettings ->
        CalendarRoute(snackbarHostState = host, onOpenSettings = openSettings)
    },
    recommend = { RecommendRoute(onOpenCalendar = it) },
    leave = { host, openRecommend ->
        LeaveRoute(snackbarHostState = host, onOpenRecommend = openRecommend)
    },
    settings = { host, back -> SettingsRoute(snackbarHostState = host, onBack = back) },
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val player = (application as YhApplication).sfxPlayer
        setContent {
            CompositionLocalProvider(LocalSfxPlayer provides player) {
                AppRoot(appViewModel, Screens)
            }
        }
    }
}
