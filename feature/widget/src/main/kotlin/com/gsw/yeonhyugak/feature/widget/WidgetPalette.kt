package com.gsw.yeonhyugak.feature.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import com.gsw.yeonhyugak.core.ui.theme.YhColors
import com.gsw.yeonhyugak.core.ui.theme.YhThemes

// 앱 테마 토큰을 위젯 색으로. 라이트/다크는 시스템이 고른다
class WidgetPalette(private val light: YhColors, private val dark: YhColors) {
    val background get() = pick { it.background }
    val ink get() = pick { it.ink }
    val muted get() = pick { it.muted }
    val holiday get() = pick { it.holiday }
    val saturday get() = pick { it.saturday }
    val accent get() = pick { it.accent }
    val leaveFill get() = pick { it.leaveFill }
    val onLeaveFill get() = pick { it.onLeaveFill }
    val halfLeaveFill get() = pick { it.halfLeaveFill }

    private fun pick(token: (YhColors) -> Color): ColorProvider =
        ColorProvider(day = token(light), night = token(dark))

    companion object {
        fun of(appearance: Appearance, month: Int): WidgetPalette {
            val spec = YhThemes.of(appearance.theme)
            return WidgetPalette(
                light = spec.colors(dark = false, month = month, cheer = appearance.cheer),
                dark = spec.colors(dark = true, month = month, cheer = appearance.cheer),
            )
        }
    }
}
