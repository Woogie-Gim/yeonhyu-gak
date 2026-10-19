package com.gsw.yeonhyugak.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.gsw.yeonhyugak.R
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.core.ui.theme.yhThemeId

const val APP_ROOT_TAG = "yh_root"

fun tabTag(tab: YhTab): String = "tab_${tab.name.lowercase()}"

enum class YhTab(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    CALENDAR(R.string.tab_calendar, R.drawable.ic_tab_calendar),
    RECOMMEND(R.string.tab_recommend, R.drawable.ic_tab_recommend),
    LEAVE(R.string.tab_leave, R.drawable.ic_tab_leave),
}

// 화면 내용은 호출한 쪽에서 넣는다 (테스트에서 ViewModel 주입)
class YhScreens(
    val onboarding: @Composable (onDone: () -> Unit) -> Unit = {},
    val calendar: @Composable (SnackbarHostState, openSettings: () -> Unit) -> Unit = { _, _ -> },
    val recommend: @Composable (openCalendar: () -> Unit) -> Unit = {},
    val leave: @Composable (SnackbarHostState, openRecommend: () -> Unit) -> Unit = { _, _ -> },
    val settings: @Composable (SnackbarHostState, back: () -> Unit) -> Unit = { _, _ -> },
)

// onboardingDone: null = 읽는 중(빈 배경만, 깜빡임 방지) · false = 첫 실행 온보딩 · true = 탭
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun YhApp(
    screens: YhScreens,
    modifier: Modifier = Modifier,
    onboardingDone: Boolean? = true,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var tab by rememberSaveable { mutableStateOf(YhTab.CALENDAR) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val themeId = YhTheme.spec.id
    val root = modifier
        .fillMaxSize()
        .semantics {
            testTagsAsResourceId = true
            yhThemeId = themeId
        }
        .testTag(APP_ROOT_TAG)
    when (onboardingDone) {
        null -> Box(root.background(YhTheme.colors.background))

        // 온보딩이 끝나면 바로 추천 화면
        false -> Scaffold(
            modifier = root,
            containerColor = YhTheme.colors.background,
            contentColor = YhTheme.colors.ink,
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding).fillMaxSize()) {
                screens.onboarding { tab = YhTab.RECOMMEND }
            }
        }

        true -> Scaffold(
            modifier = root,
            containerColor = YhTheme.colors.background,
            contentColor = YhTheme.colors.ink,
            snackbarHost = { YhSnackbarHost(snackbarHostState) },
            bottomBar = {
                // 설정 화면에서는 탭을 숨긴다
                if (!settingsOpen) YhBottomBar(tab) { tab = it }
            },
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding).fillMaxSize()) {
                if (settingsOpen) {
                    BackHandler { settingsOpen = false }
                    screens.settings(snackbarHostState) { settingsOpen = false }
                } else {
                    when (tab) {
                        YhTab.CALENDAR -> screens.calendar(snackbarHostState) {
                            settingsOpen = true
                        }

                        YhTab.RECOMMEND -> screens.recommend { tab = YhTab.CALENDAR }

                        YhTab.LEAVE -> screens.leave(snackbarHostState) { tab = YhTab.RECOMMEND }
                    }
                }
            }
        }
    }
}

@Composable
private fun YhBottomBar(tab: YhTab, onSelect: (YhTab) -> Unit) {
    NavigationBar(containerColor = YhTheme.colors.background) {
        YhTab.entries.forEach { item ->
            NavigationBarItem(
                selected = tab == item,
                onClick = { onSelect(item) },
                icon = { Icon(painterResource(item.icon), contentDescription = null) },
                label = { Text(stringResource(item.label)) },
                modifier = Modifier.testTag(tabTag(item)),
            )
        }
    }
}

// Undo 스낵바: 테마 토큰 색
@Composable
private fun YhSnackbarHost(state: SnackbarHostState) {
    val colors = YhTheme.colors
    SnackbarHost(state) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = colors.snackbarContainer,
            contentColor = colors.snackbarText,
            actionColor = colors.snackbarAction,
        )
    }
}
