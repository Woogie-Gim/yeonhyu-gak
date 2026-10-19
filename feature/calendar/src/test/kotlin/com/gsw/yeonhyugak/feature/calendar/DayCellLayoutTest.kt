package com.gsw.yeonhyugak.feature.calendar

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.engine.calendar.BlockBand
import com.gsw.yeonhyugak.core.engine.calendar.DayCell
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.holiday.HolidaySource
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.feature.calendar.CalendarHarness.Companion.may
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 날짜 숫자 기준선 · 연휴 띠 위치
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class DayCellLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dayNumber_alignedWithHolidayName_100() = assertAligned(fontScale = 1f)

    @Test
    fun dayNumber_alignedWithHolidayName_200() = assertAligned(fontScale = 2f)

    private fun assertAligned(fontScale: Float) {
        val holiday = Holiday(
            may(5),
            "어린이날",
            HolidayKind.NATIONAL,
            confirmed = true,
            source = HolidaySource.BUNDLED,
        )
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                YhTheme(darkTheme = false) {
                    Row(Modifier.height(ROW)) {
                        listOf(cell(may(5), listOf(holiday)), cell(may(6), emptyList())).forEach {
                            DayCellView(
                                cell = it,
                                isToday = false,
                                hasMemo = false,
                                selected = false,
                                modifier = Modifier.width(ROW).height(ROW),
                            )
                        }
                    }
                }
            }
        }
        val named = rule.onNodeWithTag(dayNumberTag(may(5))).getBoundsInRoot()
        val plain = rule.onNodeWithTag(dayNumberTag(may(6))).getBoundsInRoot()
        assertEquals(named.top.value, plain.top.value, TOLERANCE)
        assertEquals(named.bottom.value, plain.bottom.value, TOLERANCE)
    }

    // 띠는 자기 주 숫자에 붙고, 다음 주와는 더 떨어진다
    @Test
    fun band_closerToOwnWeek_52dp_100() = assertBandHugsOwnWeek(52.dp, 1f)

    @Test
    fun band_closerToOwnWeek_96dp_100() = assertBandHugsOwnWeek(96.dp, 1f)

    @Test
    fun band_closerToOwnWeek_52dp_200() = assertBandHugsOwnWeek(52.dp, 2f)

    @Test
    fun band_closerToOwnWeek_96dp_200() = assertBandHugsOwnWeek(96.dp, 2f)

    private fun assertBandHugsOwnWeek(row: Dp, fontScale: Float) {
        val band = BlockBand(
            length = 3,
            isStart = true,
            isEnd = false,
            labelHere = true,
            continuesPrev = false,
            continuesNext = false,
        )
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                YhTheme(darkTheme = false) {
                    Column {
                        listOf(cell(may(1), emptyList(), band), cell(may(8), emptyList())).forEach {
                            DayCellView(
                                cell = it,
                                isToday = false,
                                hasMemo = false,
                                selected = false,
                                modifier = Modifier.width(ROW).height(row),
                            )
                        }
                    }
                }
            }
        }
        val number = rule.onNodeWithTag(dayNumberTag(may(1))).getBoundsInRoot()
        val line = rule.onNodeWithTag(bandTag(may(1))).getBoundsInRoot()
        val next = rule.onNodeWithTag(dayNumberTag(may(8))).getBoundsInRoot()
        val above = line.top - number.bottom
        val below = next.top - line.bottom
        assertTrue("띠 위 $above · 띠 아래 $below", below > above)
    }

    private fun cell(date: LocalDate, holidays: List<Holiday>, band: BlockBand? = null) = DayCell(
        date = date,
        inMonth = true,
        holidays = holidays,
        leave = null,
        isWorkday = holidays.isEmpty(),
        band = band,
    )

    private companion object {
        val ROW = 52.dp
        const val TOLERANCE = 0.5f
    }
}
