package com.gsw.yeonhyugak.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import com.gsw.yeonhyugak.core.ui.theme.YhTheme

// 설정을 읽어 테마를 씌운다. MainActivity와 테스트 하네스가 같이 쓴다
@Composable
fun AppRoot(
    viewModel: AppViewModel,
    screens: YhScreens,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    val done by viewModel.onboardingDone.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val openDate by viewModel.openDate.collectAsStateWithLifecycle()
    YhTheme(
        darkTheme = darkTheme,
        appearance = appearance ?: Appearance(),
        month = viewModel.month,
    ) {
        // 설정을 다 읽기 전에는 빈 배경 (테마 깜빡임 방지)
        YhApp(
            screens = screens,
            onboardingDone = done?.takeIf { appearance != null },
            openDate = openDate,
        )
    }
}
