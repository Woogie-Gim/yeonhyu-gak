package com.gsw.yeonhyugak.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

// 연휴 띠·라벨은 배경 대비 3:1 이상 (WCAG 비텍스트·큰 글자 기준)
class YhColorsContrastTest {
    private val themes = mapOf("light" to NoteLightColors, "dark" to NoteDarkColors)

    @Test
    fun blockBand_onBackground_atLeast3() = themes.forEach { (name, c) ->
        assertContrast("$name blockBand", c.blockBand, c.background)
    }

    @Test
    fun bandLabel_onBackground_atLeast3() = themes.forEach { (name, c) ->
        assertContrast("$name ink", c.ink, c.background)
    }

    private fun assertContrast(label: String, fg: Color, bg: Color) {
        val ratio = contrast(fg, bg)
        assertTrue("$label 대비 ${"%.2f".format(ratio)}:1 < $MIN:1", ratio >= MIN)
    }

    private fun contrast(a: Color, b: Color): Float {
        val hi = maxOf(a.luminance(), b.luminance())
        val lo = minOf(a.luminance(), b.luminance())
        return (hi + OFFSET) / (lo + OFFSET)
    }

    private companion object {
        const val MIN = 3f
        const val OFFSET = 0.05f
    }
}
