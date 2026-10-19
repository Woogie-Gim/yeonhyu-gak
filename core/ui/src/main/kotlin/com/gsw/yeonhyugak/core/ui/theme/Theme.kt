package com.gsw.yeonhyugak.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId

// 지금 적용된 테마. month = 색을 정한 달 (계절 테마)
@Immutable
data class YhThemeState(
    val spec: YhThemeSpec,
    val appearance: Appearance,
    val dark: Boolean,
    val month: Int,
) {
    val colors: YhColors get() = spec.colors(dark, month, appearance.cheer)
}

private val LocalYhThemeState = staticCompositionLocalOf {
    YhThemeState(YhThemes.Note, Appearance(), dark = false, month = 1)
}
private val LocalYhColors = staticCompositionLocalOf { NoteLightColors }

object YhTheme {
    val colors: YhColors
        @Composable
        @ReadOnlyComposable
        get() = LocalYhColors.current

    val state: YhThemeState
        @Composable
        @ReadOnlyComposable
        get() = LocalYhThemeState.current

    val spec: YhThemeSpec
        @Composable
        @ReadOnlyComposable
        get() = LocalYhThemeState.current.spec

    val fonts: YhFonts
        @Composable
        @ReadOnlyComposable
        get() = LocalYhThemeState.current.spec.fonts
}

// 테마는 설정(DataStore) 값이 바뀌면 바로 다시 그린다. 재시작 필요 없음
@Composable
fun YhTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appearance: Appearance = Appearance(),
    month: Int = 1,
    content: @Composable () -> Unit,
) {
    val state = YhThemeState(YhThemes.of(appearance.theme), appearance, darkTheme, month)
    val colors = state.colors
    val typography = remember(state.spec) { typographyOf(state.spec.fonts) }
    CompositionLocalProvider(
        LocalYhThemeState provides state,
        LocalYhColors provides colors,
    ) {
        MaterialTheme(
            colorScheme = colorSchemeOf(colors, darkTheme),
            typography = typography,
            content = content,
        )
    }
}

// 계절 테마: 달력 페이지마다 그 달 색으로
@Composable
fun ProvideMonthColors(month: Int, content: @Composable () -> Unit) {
    val state = LocalYhThemeState.current.copy(month = month)
    CompositionLocalProvider(
        LocalYhThemeState provides state,
        LocalYhColors provides state.colors,
        content = content,
    )
}

// M3 컴포넌트도 토큰을 따르게 매핑
private fun colorSchemeOf(colors: YhColors, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.background,
        onSurface = colors.ink,
        surfaceContainerLowest = colors.background,
        surfaceContainerLow = colors.background,
        surfaceContainer = colors.background,
        surfaceContainerHigh = colors.background,
        surfaceContainerHighest = colors.background,
        // 하단 탭 선택 글자 등. 기본값은 테마 바탕(야구 초록)에서 대비가 낮다
        secondary = colors.ink,
        onSecondary = colors.background,
        secondaryContainer = colors.outline,
        onSecondaryContainer = colors.ink,
        onSurfaceVariant = colors.muted,
        outline = colors.outline,
        // 스낵바
        inverseSurface = colors.snackbarContainer,
        inverseOnSurface = colors.snackbarText,
        inversePrimary = colors.snackbarAction,
    )
}

// 테스트에서 지금 테마 확인용
val YhThemeIdKey = SemanticsPropertyKey<ThemeId>("YhThemeId")
var SemanticsPropertyReceiver.yhThemeId by YhThemeIdKey
