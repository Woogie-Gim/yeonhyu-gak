package com.gsw.yeonhyugak.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import org.junit.Assert.assertTrue
import org.junit.Test

// 모든 테마 × 라이트/다크 × (계절 12개월) × (야구 응원 색 6개)
// 글자 4.5:1 · 띠·채움·링 같은 그래픽 3:1 (WCAG 1.4.3 · 1.4.11)
class YhColorsContrastTest {
    private data class Case(val label: String, val colors: YhColors)

    private val cases: List<Case> = YhThemes.all.flatMap { spec ->
        listOf(false, true).flatMap { dark ->
            val mode = if (dark) "dark" else "light"
            when (spec.id) {
                ThemeId.SEASON -> (1..12).map { m ->
                    Case("${spec.id}/$mode/${m}월", spec.colors(dark, month = m))
                }

                ThemeId.BASEBALL -> CheerColor.entries.map { c ->
                    Case("${spec.id}/$mode/$c", spec.colors(dark, cheer = c))
                }

                else -> listOf(Case("${spec.id}/$mode", spec.colors(dark)))
            }
        }
    }

    private val textPairs: List<Pair<String, (YhColors) -> Pair<Color, Color>>> = listOf(
        "ink↔background" to { c -> c.ink to c.background },
        "muted↔background" to { c -> c.muted to c.background },
        "holiday↔background" to { c -> c.holiday to c.background },
        "saturday↔background" to { c -> c.saturday to c.background },
        "accent↔background" to { c -> c.accent to c.background },
        "onAccent↔accent" to { c -> c.onAccent to c.accent },
        "warning↔background" to { c -> c.warning to c.background },
        "onWarning↔warning" to { c -> c.onWarning to c.warning },
        "bandLabel↔background" to { c -> c.bandLabel to c.background },
        "todayText↔background" to { c -> c.todayText to c.background },
        "onLeaveFill↔leaveFill" to { c -> c.onLeaveFill to c.leaveFill },
        "ink↔halfLeaveFill" to { c -> c.ink to c.halfLeaveFill },
        "holiday↔halfLeaveFill" to { c -> c.holiday to c.halfLeaveFill },
        "saturday↔halfLeaveFill" to { c -> c.saturday to c.halfLeaveFill },
        "snackbarText↔snackbarContainer" to { c -> c.snackbarText to c.snackbarContainer },
        "snackbarAction↔snackbarContainer" to { c -> c.snackbarAction to c.snackbarContainer },
        // 월별 배경 그림 위에 글자가 겹쳐도
        "ink↔backdropInk" to { c -> c.ink to c.backdropInk },
        "muted↔backdropInk" to { c -> c.muted to c.backdropInk },
        "holiday↔backdropInk" to { c -> c.holiday to c.backdropInk },
        "saturday↔backdropInk" to { c -> c.saturday to c.backdropInk },
        "bandLabel↔backdropInk" to { c -> c.bandLabel to c.backdropInk },
    )

    private val graphicPairs: List<Pair<String, (YhColors) -> Pair<Color, Color>>> = listOf(
        "bandFill↔background" to { c -> c.bandFill to c.background },
        "bandFill↔backdropInk" to { c -> c.bandFill to c.backdropInk },
        "leaveFill↔background" to { c -> c.leaveFill to c.background },
        "leaveFill↔backdropInk" to { c -> c.leaveFill to c.backdropInk },
        // 반차 반쪽 테두리는 leaveFill로 그린다
        "leaveFill↔halfLeaveFill" to { c -> c.leaveFill to c.halfLeaveFill },
        "todayRing↔background" to { c -> c.todayRing to c.background },
        "todayRingOnLeave↔leaveFill" to { c -> c.todayRingOnLeave to c.leaveFill },
        // 예상 공휴일 점선 · 선택 테두리 · 메모 점
        "holiday↔background(점선)" to { c -> c.holiday to c.background },
        "accent↔background(선택)" to { c -> c.accent to c.background },
        "memoDot↔background" to { c -> c.memoDot to c.background },
    )

    @Test
    fun caseCount_coversAllThemesAndModes() {
        // 노트·아날로그·Lo-fi 3×2 + 계절 12×2 + 야구 6×2
        assertTrue(cases.size == 3 * 2 + 12 * 2 + 6 * 2)
    }

    @Test
    fun text_atLeast45() = assertAll(textPairs, TEXT_MIN)

    @Test
    fun graphic_atLeast3() = assertAll(graphicPairs, GRAPHIC_MIN)

    private fun assertAll(pairs: List<Pair<String, (YhColors) -> Pair<Color, Color>>>, min: Float) {
        val failures = cases.flatMap { case ->
            pairs.mapNotNull { (name, pick) ->
                val (fg, bg) = pick(case.colors)
                val ratio = contrast(fg, bg)
                if (ratio < min) "${case.label} $name ${"%.2f".format(ratio)}:1" else null
            }
        }
        assertTrue("$min:1 미만\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun contrast(a: Color, b: Color): Float {
        val hi = maxOf(a.luminance(), b.luminance())
        val lo = minOf(a.luminance(), b.luminance())
        return (hi + OFFSET) / (lo + OFFSET)
    }

    private companion object {
        const val TEXT_MIN = 4.5f
        const val GRAPHIC_MIN = 3f
        const val OFFSET = 0.05f
    }
}
