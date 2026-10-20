package com.gsw.yeonhyugak.core.data.widget

// 홈 화면 위젯 다시 그리기. 구현은 :feature:widget
fun interface WidgetRefresher {
    suspend fun refresh()
}
