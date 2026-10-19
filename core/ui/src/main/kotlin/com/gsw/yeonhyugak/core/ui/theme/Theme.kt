package com.gsw.yeonhyugak.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalYhColors = staticCompositionLocalOf { NoteLightColors }

object YhTheme {
    val colors: YhColors
        @Composable
        @ReadOnlyComposable
        get() = LocalYhColors.current
}

// 테마 시스템 전체는 Phase 4. 지금은 기본 노트만 제공
@Composable
fun YhTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) NoteDarkColors else NoteLightColors
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    // M3 컴포넌트도 토큰을 따르게 매핑
    val colorScheme = base.copy(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.background,
        onSurface = colors.ink,
        surfaceContainerLow = colors.background,
        surfaceContainer = colors.background,
        surfaceContainerHigh = colors.background,
        secondaryContainer = colors.outline,
        onSecondaryContainer = colors.ink,
        onSurfaceVariant = colors.muted,
        outline = colors.outline,
        inverseSurface = colors.ink,
        inverseOnSurface = colors.background,
    )
    CompositionLocalProvider(LocalYhColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
