package com.gsw.yeonhyugak.dev

import android.app.Application
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.feature.calendar.CALENDAR_SETTINGS_TAG
import com.gsw.yeonhyugak.feature.settings.SETTINGS_DEV_TAG
import com.gsw.yeonhyugak.feature.settings.SETTINGS_TAG
import com.gsw.yeonhyugak.ui.AppHarness
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 디버그 빌드: 설정 맨 아래 개발자 메뉴 · 버튼 3개가 각각 바로 실행
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class DevMenuDebugTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun debugBuild_hasDevMenu() {
        assertNotNull(DevMenus.entry)
    }

    @Test
    fun settings_showsDevRow_opensMenu() {
        val harness = AppHarness(devMenu = DevMenus.entry).apply { onboarded() }
        try {
            rule.setContent { harness.App() }
            waitFor(CALENDAR_SETTINGS_TAG)
            rule.onNodeWithTag(CALENDAR_SETTINGS_TAG).performClick()
            waitFor(SETTINGS_TAG)
            rule.onNodeWithTag(SETTINGS_TAG).performScrollToNode(hasTestTag(SETTINGS_DEV_TAG))
            waitFor(SETTINGS_DEV_TAG)
            rule.onNodeWithTag(SETTINGS_DEV_TAG).assert(hasText("개발자 메뉴"))
            rule.onNodeWithTag(SETTINGS_DEV_TAG).performClick()
            waitFor(DEV_ALERTS_TAG)
            waitFor(DEV_SYNC_TAG)
            waitFor(DEV_WIDGETS_TAG)
        } finally {
            harness.close()
        }
    }

    @Test
    fun buttons_runEachActionImmediately() {
        val calls = mutableListOf<String>()
        val actions = DevActions(
            sendAlerts = { "alerts".also(calls::add) },
            syncHolidays = { "sync".also(calls::add) },
            refreshWidgets = { "widgets".also(calls::add) },
        )
        rule.setContent { YhTheme(darkTheme = false) { DevMenuScreen(actions) {} } }

        rule.onNodeWithTag(DEV_ALERTS_TAG).performClick()
        rule.waitUntil(TIMEOUT) { calls == listOf("alerts") }
        rule.onNodeWithTag(DEV_RESULT_TAG).assert(hasText("alerts"))

        rule.onNodeWithTag(DEV_SYNC_TAG).performClick()
        rule.waitUntil(TIMEOUT) { calls.size == 2 }
        rule.onNodeWithTag(DEV_WIDGETS_TAG).performScrollTo().performClick()
        rule.waitUntil(TIMEOUT) { calls.size == 3 }
        assertEquals(listOf("alerts", "sync", "widgets"), calls)
    }

    private fun waitFor(tag: String) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
