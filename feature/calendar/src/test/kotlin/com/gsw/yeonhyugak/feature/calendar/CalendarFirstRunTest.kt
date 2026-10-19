package com.gsw.yeonhyugak.feature.calendar

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.feature.calendar.CalendarHarness.Companion.may
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 첫 진입 코치마크 · 메모 되돌리기 · 초과 경고
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class CalendarFirstRunTest {
    @get:Rule
    val rule = createComposeRule()

    private val harness = CalendarHarness(coachMarkShown = false)

    @After
    fun tearDown() = harness.close()

    private fun launch() {
        rule.setContent {
            YhTheme(darkTheme = false) {
                val host = remember { SnackbarHostState() }
                Box(Modifier.fillMaxSize()) {
                    CalendarRoute(snackbarHostState = host, viewModel = harness.viewModel())
                    SnackbarHost(host, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        rule.waitUntil(TIMEOUT) { exists(hasTestTag(cellTag(may(1)))) }
    }

    private fun exists(matcher: SemanticsMatcher): Boolean =
        rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun coachMark_showsOnceThenRemembered() {
        launch()
        rule.onNodeWithText("꾹 누른 채 밀면 여러 날을 한 번에 지정해요").assertExists()
        rule.onNodeWithText("알겠어요").performClick()
        rule.waitUntil(TIMEOUT) { !exists(hasTestTag(CALENDAR_COACH_TAG)) }
        assertTrue(runBlocking { harness.prefs.coachMarkShown.first() })
    }

    @Test
    fun memoUndo_restoresPreviousText() {
        runBlocking { harness.memos.save(may(7), "처음 메모") }
        launch()
        rule.onNodeWithTag(cellTag(may(7))).performTouchInput { longClick() }
        rule.waitUntil(TIMEOUT) { exists(hasText("메모 지우기")) }
        // 시트 열림 애니메이션이 끝난 뒤 누른다
        rule.waitForIdle()
        rule.onNodeWithText("메모 지우기").assertIsDisplayed().performClick()
        rule.waitUntil(TIMEOUT) { !exists(hasTestTag(DAY_SHEET_TAG)) }
        rule.waitUntil(TIMEOUT) { harness.memoText(may(7)) == null }
        rule.waitUntil(TIMEOUT) { exists(hasText("되돌리기")) }
        rule.onNodeWithText("되돌리기").performClick()
        rule.waitUntil(TIMEOUT) { harness.memoText(may(7)) == "처음 메모" }
    }

    @Test
    fun overAllowance_warnsButStillMarks() {
        runBlocking { harness.prefs.setAllowance(1.0) }
        launch()
        // 5/18(월) ~ 5/20(수): 3일 차감, 허용 1일 → 2일 초과
        val grid = rule.onNodeWithTag(CALENDAR_GRID_TAG).fetchSemanticsNode().boundsInRoot
        val a =
            rule.onNodeWithTag(cellTag(may(18))).fetchSemanticsNode().boundsInRoot.center -
                grid.topLeft
        val b =
            rule.onNodeWithTag(cellTag(may(20))).fetchSemanticsNode().boundsInRoot.center -
                grid.topLeft
        rule.onNodeWithTag(CALENDAR_GRID_TAG).performTouchInput {
            down(a)
            advanceEventTime(LONG_PRESS_MS + 100)
            moveTo(b)
        }
        rule.waitUntil(TIMEOUT) { exists(hasText("2일 초과", substring = true)) }
        rule.onNodeWithTag(CALENDAR_GRID_TAG).performTouchInput { up() }
        rule.waitUntil(TIMEOUT) { harness.leaveMap().size == 3 }
        assertTrue(harness.leaveMap().values.all { it == LeaveType.FULL })
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
