package com.gsw.yeonhyugak.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.R
import com.gsw.yeonhyugak.core.ui.theme.YhTheme

const val APP_ROOT_TAG = "yh_root"

fun tabTag(tab: YhTab): String = "tab_${tab.name.lowercase()}"

enum class YhTab(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    CALENDAR(R.string.tab_calendar, R.drawable.ic_tab_calendar),
    RECOMMEND(R.string.tab_recommend, R.drawable.ic_tab_recommend),
    LEAVE(R.string.tab_leave, R.drawable.ic_tab_leave),
}

// 하단 탭 3개. 달력 내용은 호출한 쪽에서 넣는다 (테스트에서 ViewModel 주입)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun YhApp(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    calendar: @Composable (SnackbarHostState) -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(YhTab.CALENDAR) }
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag(APP_ROOT_TAG),
        containerColor = YhTheme.colors.background,
        contentColor = YhTheme.colors.ink,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = YhTheme.colors.background) {
                YhTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(painterResource(item.icon), contentDescription = null) },
                        label = { Text(stringResource(item.label)) },
                        modifier = Modifier.testTag(tabTag(item)),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            when (tab) {
                YhTab.CALENDAR -> calendar(snackbarHostState)

                YhTab.RECOMMEND -> EmptyState(
                    title = R.string.empty_recommend_title,
                    body = R.string.empty_recommend_body,
                    onGoCalendar = { tab = YhTab.CALENDAR },
                )

                YhTab.LEAVE -> EmptyState(
                    title = R.string.empty_leave_title,
                    body = R.string.empty_leave_body,
                    onGoCalendar = { tab = YhTab.CALENDAR },
                )
            }
        }
    }
}

// 빈 화면은 다음 행동을 제안 (SPEC 9장)
@Composable
private fun EmptyState(@StringRes title: Int, @StringRes body: Int, onGoCalendar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = YhTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onGoCalendar, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.empty_go_calendar))
        }
    }
}
