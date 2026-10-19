package com.gsw.yeonhyugak.core.ui.theme

import androidx.annotation.RawRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.ui.R

// 칸 모서리
@Immutable
data class CellStyle(val corner: Dp)

// 월별 배경 종류 (MonthBackdrop)
enum class BackdropKind { NONE, SEASON, GRID, WINDOW, SCOREBOARD }

// 월 넘김 효과 (monthTransition)
enum class MonthTransition { SLIDE, FADE_SLIDE, PAPER, GRAIN_FADE, FLIP }

enum class SfxEffect { PAGE, MARK, UNMARK, SUCCESS }

@Immutable
data class SoundPack(
    @param:RawRes val page: Int,
    @param:RawRes val mark: Int,
    @param:RawRes val unmark: Int,
    @param:RawRes val success: Int,
) {
    @RawRes
    fun of(effect: SfxEffect): Int = when (effect) {
        SfxEffect.PAGE -> page
        SfxEffect.MARK -> mark
        SfxEffect.UNMARK -> unmark
        SfxEffect.SUCCESS -> success
    }
}

// 테마 = 색 · 숫자/제목 폰트 · 칸 모양 · 배경 · 월 넘김 · 소리 (SPEC 7장)
// 계절은 월마다, 야구는 응원 색마다 색이 달라서 색은 함수로 둔다
@Immutable
class YhThemeSpec internal constructor(
    val id: ThemeId,
    val fonts: YhFonts,
    val cellStyle: CellStyle,
    val backdrop: BackdropKind,
    val transition: MonthTransition,
    val soundPack: SoundPack,
    private val palette: (dark: Boolean, month: Int, cheer: CheerColor) -> YhColors,
) {
    fun colors(dark: Boolean, month: Int = 1, cheer: CheerColor = CheerColor.RED): YhColors =
        palette(dark, month, cheer)

    val light: YhColors get() = colors(dark = false)
    val dark: YhColors get() = colors(dark = true)
}

object YhThemes {
    val Note = YhThemeSpec(
        id = ThemeId.NOTE,
        fonts = YhFonts(numeral = BodyFont, title = BodyFont),
        cellStyle = CellStyle(corner = 8.dp),
        backdrop = BackdropKind.NONE,
        transition = MonthTransition.SLIDE,
        soundPack = SoundPack(
            R.raw.sfx_note_page,
            R.raw.sfx_note_mark,
            R.raw.sfx_note_unmark,
            R.raw.sfx_note_success,
        ),
        palette = { dark, _, _ -> if (dark) NoteDarkColors else NoteLightColors },
    )

    val Season = YhThemeSpec(
        id = ThemeId.SEASON,
        fonts = YhFonts(numeral = BodyFont, title = BodyFont),
        cellStyle = CellStyle(corner = 10.dp),
        backdrop = BackdropKind.SEASON,
        transition = MonthTransition.FADE_SLIDE,
        soundPack = SoundPack(
            R.raw.sfx_season_page,
            R.raw.sfx_season_mark,
            R.raw.sfx_season_unmark,
            R.raw.sfx_season_success,
        ),
        palette = { dark, month, _ -> seasonColors(month, dark) },
    )

    val Analog = YhThemeSpec(
        id = ThemeId.ANALOG,
        fonts = YhFonts(numeral = PenFont, title = PenFont),
        cellStyle = CellStyle(corner = 2.dp),
        backdrop = BackdropKind.GRID,
        transition = MonthTransition.PAPER,
        soundPack = SoundPack(
            R.raw.sfx_analog_page,
            R.raw.sfx_analog_mark,
            R.raw.sfx_analog_unmark,
            R.raw.sfx_analog_success,
        ),
        palette = { dark, _, _ -> if (dark) AnalogDarkColors else AnalogLightColors },
    )

    val Lofi = YhThemeSpec(
        id = ThemeId.LOFI,
        fonts = YhFonts(numeral = PixelFont, title = PixelFont),
        cellStyle = CellStyle(corner = 12.dp),
        backdrop = BackdropKind.WINDOW,
        transition = MonthTransition.GRAIN_FADE,
        soundPack = SoundPack(
            R.raw.sfx_lofi_page,
            R.raw.sfx_lofi_mark,
            R.raw.sfx_lofi_unmark,
            R.raw.sfx_lofi_success,
        ),
        palette = { dark, _, _ -> if (dark) LofiDarkColors else LofiLightColors },
    )

    // 한글 제목은 Doto에 글자가 없어 Pretendard
    val Baseball = YhThemeSpec(
        id = ThemeId.BASEBALL,
        fonts = YhFonts(numeral = DotFont, title = BodyFont),
        cellStyle = CellStyle(corner = 2.dp),
        backdrop = BackdropKind.SCOREBOARD,
        transition = MonthTransition.FLIP,
        soundPack = SoundPack(
            R.raw.sfx_baseball_page,
            R.raw.sfx_baseball_mark,
            R.raw.sfx_baseball_unmark,
            R.raw.sfx_baseball_success,
        ),
        palette = { dark, _, cheer -> baseballColors(cheer, dark) },
    )

    val all: List<YhThemeSpec> = listOf(Note, Season, Analog, Lofi, Baseball)

    fun of(id: ThemeId): YhThemeSpec = all.first { it.id == id }
}
