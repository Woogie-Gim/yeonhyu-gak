package com.gsw.yeonhyugak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.feature.calendar.CalendarRoute
import com.gsw.yeonhyugak.feature.leave.LeaveRoute
import com.gsw.yeonhyugak.feature.recommend.RecommendRoute
import com.gsw.yeonhyugak.feature.recommend.onboarding.OnboardingRoute
import com.gsw.yeonhyugak.ui.AppViewModel
import com.gsw.yeonhyugak.ui.YhApp
import com.gsw.yeonhyugak.ui.YhScreens
import dagger.hilt.android.AndroidEntryPoint

private val Screens = YhScreens(
    onboarding = { OnboardingRoute(onDone = it) },
    calendar = { CalendarRoute(snackbarHostState = it) },
    recommend = { RecommendRoute(onOpenCalendar = it) },
    leave = { host, openRecommend ->
        LeaveRoute(snackbarHostState = host, onOpenRecommend = openRecommend)
    },
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val onboardingDone by appViewModel.onboardingDone.collectAsStateWithLifecycle()
            YhTheme {
                YhApp(screens = Screens, onboardingDone = onboardingDone)
            }
        }
    }
}
