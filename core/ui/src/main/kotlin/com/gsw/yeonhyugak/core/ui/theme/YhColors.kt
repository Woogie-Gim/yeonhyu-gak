package com.gsw.yeonhyugak.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// 색 토큰. 화면에서는 YhTheme.colors로만 접근
// 글자 토큰은 바탕 대비 4.5:1, 그래픽 토큰은 3:1 이상 (YhColorsContrastTest)
@Immutable
data class YhColors(
    val background: Color,
    val ink: Color,
    val muted: Color,
    val outline: Color,
    val holiday: Color,
    val saturday: Color,
    // 버튼·강조 글자. 바탕 위 글자로도 쓴다
    val accent: Color,
    val onAccent: Color,
    val warning: Color,
    val onWarning: Color,
    // 연휴 띠 · `n일` 라벨
    val bandFill: Color,
    val bandLabel: Color,
    // 연차 칸 채움 · 그 위 글자 · 반차 반쪽 채움(불투명)
    val leaveFill: Color,
    val onLeaveFill: Color,
    val halfLeaveFill: Color,
    // 오늘 표시 링 · 연차 칸 위 링 · 오늘 버튼 글자
    val todayRing: Color,
    val todayRingOnLeave: Color,
    val todayText: Color,
    // Undo 스낵바
    val snackbarContainer: Color,
    val snackbarText: Color,
    val snackbarAction: Color,
    val memoDot: Color,
    // 월별 배경 그림. 글자가 겹쳐도 4.5:1 유지
    val backdropInk: Color,
    // 장식 색 (야구 응원 줄). 정보 전달에 쓰지 않는다
    val decor: Color = accent,
)

// 계절 테마 월별 색
internal data class MonthTint(
    val accent: Long,
    val leaveFill: Long,
    val bandFill: Long,
    val halfLeaveFill: Long,
    val backdropInk: Long,
    val snackbarAction: Long,
)

internal fun YhColors.tinted(t: MonthTint): YhColors = copy(
    accent = Color(t.accent),
    leaveFill = Color(t.leaveFill),
    bandFill = Color(t.bandFill),
    halfLeaveFill = Color(t.halfLeaveFill),
    backdropInk = Color(t.backdropInk),
    snackbarAction = Color(t.snackbarAction),
    decor = Color(t.leaveFill),
)
