package com.gsw.yeonhyugak.ui

import android.app.Application
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.SoundMode
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.ui.theme.YhThemeIdKey
import com.gsw.yeonhyugak.feature.calendar.CALENDAR_SETTINGS_TAG
import com.gsw.yeonhyugak.feature.calendar.cellTag
import com.gsw.yeonhyugak.feature.settings.SETTINGS_BACK_TAG
import com.gsw.yeonhyugak.feature.settings.SETTINGS_HAPTIC_TAG
import com.gsw.yeonhyugak.feature.settings.SETTINGS_TAG
import com.gsw.yeonhyugak.feature.settings.settingsCheerTag
import com.gsw.yeonhyugak.feature.settings.settingsSoundModeTag
import com.gsw.yeonhyugak.feature.settings.settingsThemeTag
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 톱니 → 테마 카드 탭 → 재시작 없이 바로 적용 + DataStore 저장 → 되돌리기
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class ThemeChangeTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var harness: AppHarness

    @After
    fun tearDown() = harness.close()

    private fun launch(setup: AppHarness.() -> Unit = {}) {
        harness = AppHarness().apply {
            onboarded()
            setup()
        }
        rule.setContent { harness.App() }
        waitFor(tabTag(YhTab.CALENDAR))
    }

    @Test
    fun pickTheme_appliesImmediately_savesAndUndoes() {
        launch()
        assertEquals(ThemeId.NOTE, rootTheme())

        openSettings()
        pick(settingsThemeTag(ThemeId.LOFI))

        // 같은 화면 그대로 테마만 바뀐다
        rule.waitUntil(TIMEOUT) { rootTheme() == ThemeId.LOFI }
        assertEquals(ThemeId.LOFI, harness.appearance().theme)
        rule.waitUntil(TIMEOUT) { textShown("Lo-fi로 바꿨어요") }

        rule.onNodeWithText("되돌리기").performClick()
        rule.waitUntil(TIMEOUT) { rootTheme() == ThemeId.NOTE }
        assertEquals(ThemeId.NOTE, harness.appearance().theme)
    }

    @Test
    fun savedTheme_appliedOnLaunch_andBackReturnsToCalendar() {
        launch { setTheme(ThemeId.ANALOG) }
        assertEquals(ThemeId.ANALOG, rootTheme())

        openSettings()
        rule.onNodeWithTag(SETTINGS_BACK_TAG).performClick()
        waitFor(CALENDAR_SETTINGS_TAG)
        assertFalse(exists(SETTINGS_TAG))
    }

    @Test
    fun cheerColor_savedWithUndo() {
        launch { setTheme(ThemeId.BASEBALL) }
        openSettings()
        // 야구 카드 아래 효과음 줄까지 내려 칩이 화면 가장자리에 걸리지 않게
        pick(settingsCheerTag(CheerColor.NAVY), anchor = settingsSoundModeTag(SoundMode.SYSTEM))
        waitSaved { harness.appearance().cheer == CheerColor.NAVY }
        rule.waitUntil(TIMEOUT) { textShown("응원 색을 남색으로 바꿨어요") }
        rule.onNodeWithText("되돌리기").performClick()
        waitSaved { harness.appearance().cheer == CheerColor.RED }
    }

    @Test
    fun soundAndHaptic_saved() {
        launch()
        openSettings()
        pick(settingsSoundModeTag(SoundMode.OFF))
        waitSaved { harness.appearance().sound == SoundMode.OFF }
        pick(SETTINGS_HAPTIC_TAG)
        waitSaved { !harness.appearance().haptic }
    }

    // 야구 테마: 연차 지정 문구가 `세이프`로 시작 (영문 SAFE 아님)
    @Test
    fun baseball_markSaysSafeInKorean() {
        launch { setTheme(ThemeId.BASEBALL) }
        val workday = LocalDate.of(2026, 5, 7)
        waitFor(cellTag(workday))
        rule.onNodeWithTag(cellTag(workday)).performClick()
        rule.waitUntil(TIMEOUT) { textShown("세이프! 연차를 지정했어요") }
        assertFalse(textShown("SAFE"))
    }

    private fun openSettings() {
        rule.onNodeWithTag(CALENDAR_SETTINGS_TAG).performClick()
        waitFor(SETTINGS_TAG)
    }

    private fun pick(tag: String, anchor: String = tag) {
        rule.onNodeWithTag(SETTINGS_TAG).performScrollToNode(hasTestTag(anchor))
        rule.onNodeWithTag(tag).performClick()
    }

    // Robolectric: waitUntil 조건 안에서 메인 루퍼를 돌려야 ViewModel 저장 코루틴이 실행된다
    private fun waitSaved(condition: () -> Boolean) = rule.waitUntil(TIMEOUT) {
        rule.waitForIdle()
        condition()
    }

    private fun rootTheme(): ThemeId? {
        rule.waitForIdle()
        return rule.onNodeWithTag(APP_ROOT_TAG).fetchSemanticsNode().config.getOrNull(YhThemeIdKey)
    }

    private fun textShown(text: String): Boolean =
        rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun exists(tag: String): Boolean =
        rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(tag: String) = rule.waitUntil(TIMEOUT) { exists(tag) }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
