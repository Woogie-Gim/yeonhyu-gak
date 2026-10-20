package com.gsw.yeonhyugak.dev

import androidx.compose.runtime.Composable

// 개발자 메뉴. 구현은 src/debug에만 있고 src/release의 DevMenus.entry는 null
interface DevMenu {
    @Composable
    fun label(): String

    @Composable
    fun Screen(onBack: () -> Unit)
}
