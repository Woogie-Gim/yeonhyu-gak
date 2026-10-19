package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gsw.yeonhyugak.core.ui.MonthBackdrop
import com.gsw.yeonhyugak.core.ui.monthTransition
import com.gsw.yeonhyugak.core.ui.rememberYhFeedback
import com.gsw.yeonhyugak.core.ui.theme.ProvideMonthColors
import com.gsw.yeonhyugak.core.ui.theme.SfxEffect
import com.gsw.yeonhyugak.core.ui.theme.YhTheme

// 월 넘김 효과음. 처음 열 때는 울리지 않는다
@Composable
internal fun PageSoundEffect(pagerState: PagerState) {
    val feedback = rememberYhFeedback()
    var lastPage by remember { mutableIntStateOf(pagerState.settledPage) }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != lastPage) feedback.sound(SfxEffect.PAGE)
        lastPage = pagerState.settledPage
    }
}

// 페이지 바탕: 그 달 색(계절 테마) · 월별 배경 · 테마 월 넘김 효과
@Composable
internal fun MonthPlate(
    pagerState: PagerState,
    page: Int,
    reduceMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val month = monthOf(page).monthValue
    val transition = YhTheme.spec.transition
    ProvideMonthColors(month) {
        val colors = YhTheme.colors
        Box(
            Modifier
                .fillMaxSize()
                .monthTransition(pagerState, page, transition, reduceMotion, colors.muted)
                .background(colors.background),
        ) {
            MonthBackdrop(month, Modifier.matchParentSize())
            content()
        }
    }
}
