package com.gsw.yeonhyugak.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// 색 토큰. 화면에서는 YhTheme.colors로만 접근
@Immutable
data class YhColors(
    val background: Color,
    val ink: Color,
    val holiday: Color,
    val saturday: Color,
    val accent: Color,
    val onAccent: Color,
)

// 기본 노트 (SPEC 7장)
internal val NoteLightColors = YhColors(
    background = Color(0xFFFFFFFF),
    ink = Color(0xFF1E2430),
    holiday = Color(0xFFD7263D),
    saturday = Color(0xFF2E5AAC),
    accent = Color(0xFF3A86FF),
    onAccent = Color(0xFFFFFFFF),
)

// 다크 변형은 임시값. Phase 4 대비 테스트에서 확정
internal val NoteDarkColors = YhColors(
    background = Color(0xFF14181F),
    ink = Color(0xFFE6E9EF),
    holiday = Color(0xFFFF7A85),
    saturday = Color(0xFF8AB4F8),
    accent = Color(0xFF6EA8FF),
    onAccent = Color(0xFF0B1220),
)
