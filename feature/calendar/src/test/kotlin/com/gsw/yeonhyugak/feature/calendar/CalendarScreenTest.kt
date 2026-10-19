package com.gsw.yeonhyugak.feature.calendar

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.feature.calendar.CalendarHarness.Companion.may
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class CalendarScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var harness: CalendarHarness

    @Before
    fun setUp() {
        harness = CalendarHarness()
        rule.setContent {
            YhTheme(darkTheme = false) {
                val host = remember { SnackbarHostState() }
                Box(Modifier.fillMaxSize()) {
                    CalendarRoute(snackbarHostState = host, viewModel = harness.viewModel())
                    SnackbarHost(host, Modifier.align(Alignment.BottomCenter))
                }
            }
        }
        rule.waitUntil(TIMEOUT) { cellExists(may(1)) }
    }

    @After
    fun tearDown() = harness.close()

    @Test
    fun tap_togglesLeave() {
        cell(may(7)).performClick()
        rule.waitUntil(TIMEOUT) { harness.leaveMap() == mapOf(may(7) to LeaveType.FULL) }
        rule.waitUntil(TIMEOUT) { description(may(7)).contains("연차") }

        cell(may(7)).performClick()
        rule.waitUntil(TIMEOUT) { harness.leaveMap().isEmpty() }
    }

    @Test
    fun drag_fiveDaysWithWeekend_deductsThree() {
        drag(may(7), may(11))
        rule.waitUntil(TIMEOUT) { harness.leaveMap().isNotEmpty() }
        assertEquals(
            listOf(may(7), may(8), may(11)).associateWith { LeaveType.FULL },
            harness.leaveMap(),
        )
        rule.onNodeWithText("연차 3일을 지정했어요").assertExists()
    }

    @Test
    fun undo_restoresRangeSelection() {
        drag(may(7), may(11))
        rule.waitUntil(TIMEOUT) { harness.leaveMap().size == 3 }
        rule.onNodeWithText("되돌리기").performClick()
        rule.waitUntil(TIMEOUT) { harness.leaveMap().isEmpty() }
    }

    @Test
    fun holidayTap_showsNameOnly() {
        cell(may(5)).performClick()
        rule.waitUntil(TIMEOUT) { bubbleHas("어린이날") }
        rule.waitForIdle()
        assertTrue(harness.leaveMap().isEmpty())
    }

    @Test
    fun remoteElection_followsSameRuleAsBundledHoliday() {
        val remote = description(CalendarHarness.REMOTE_ELECTION.date)
        val bundled = description(may(5))
        assertTrue(remote.contains("공휴일 재보궐선거"))
        assertFalse(remote.contains("예상"))
        assertFalse(bundled.contains("예상"))

        cell(CalendarHarness.REMOTE_ELECTION.date).performClick()
        rule.waitUntil(TIMEOUT) { bubbleHas("재보궐선거") }
        assertTrue(harness.leaveMap().isEmpty())
    }

    @Test
    fun dragging_doesNotChangeMonth() {
        val grid = rule.onNodeWithTag(CALENDAR_GRID_TAG)
        val start = centerInGrid(may(13))
        grid.performTouchInput {
            down(start)
            advanceEventTime(LONG_PRESS_MS + 100)
            // 왼쪽 끝 너머까지 크게 밀기
            repeat(10) { i -> moveTo(Offset(start.x - (i + 1) * 80f, start.y)) }
            up()
        }
        // 손을 떼면 그은 구간이 지정된다 (5/10 일 ~ 5/13 선거일 → 근무일 2일)
        rule.waitUntil(TIMEOUT) { harness.leaveMap().isNotEmpty() }
        assertEquals(setOf(may(11), may(12)), harness.leaveMap().keys)
        rule.onNodeWithContentDescription("2026년 5월").assertExists()
    }

    @Test
    fun memo_savedAsDotOnly_textNeverOnCalendar() {
        longPress(may(7))
        rule.onNodeWithTag(MEMO_FIELD_TAG).performTextInput("병원 예약")
        rule.onNodeWithTag(MEMO_FIELD_TAG).assert(hasText("병원 예약"))
        rule.onNodeWithText("메모 저장").assertIsEnabled()
        rule.onNodeWithText("메모 저장").performClick()
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(hasTestTag(DAY_SHEET_TAG)).fetchSemanticsNodes().isEmpty()
        }
        rule.waitUntil(TIMEOUT) { harness.memoText(may(7)) == "병원 예약" }
        rule.waitUntil(TIMEOUT) { description(may(7)).contains("메모 있음") }

        // 시트가 닫힌 뒤 화면 어디에도 메모 내용이 없다
        rule.onAllNodesWithText(
            "병원 예약",
            substring = true,
            useUnmergedTree = true,
        ).assertCountEquals(0)
        assertTrue(harness.leaveMap().isEmpty())
        assertFalse(description(may(7)).contains("병원"))
    }

    @Test
    fun weekendSheet_hasMemoButNoLeaveButtons() {
        longPress(may(9))
        rule.onNodeWithTag(MEMO_FIELD_TAG).assertExists()
        rule.onNodeWithText("오전 반차").assertDoesNotExist()
        rule.onNodeWithText("여기부터 선택").assertExists()
    }

    @Test
    fun sheet_halfDay() {
        longPress(may(7))
        rule.onNodeWithText("오후 반차").performClick()
        rule.waitUntil(TIMEOUT) { harness.leaveMap() == mapOf(may(7) to LeaveType.PM_HALF) }
        rule.waitUntil(TIMEOUT) { description(may(7)).contains("오후 반차") }
    }

    @Test
    fun talkBackRange_fromAndToActions() {
        customAction(may(7), "여기부터 선택")
        rule.waitUntil(TIMEOUT) { bubbleHas("부터 선택했어요") }
        customAction(may(11), "여기까지 선택")
        rule.waitUntil(TIMEOUT) { harness.leaveMap().size == 3 }
    }

    @Test
    fun accessibility_cellsAreLargeAndDescribed() {
        (1..31).map(::may).forEach { date ->
            val node = cell(date).fetchSemanticsNode()
            val density = node.layoutInfo.density.density
            assertTrue("$date 높이", node.size.height / density >= MIN_TOUCH_DP)
            assertTrue("$date 너비", node.size.width / density >= MIN_TOUCH_DP)
            assertTrue(
                node.config.getOrNull(
                    SemanticsProperties.ContentDescription,
                ).orEmpty().isNotEmpty(),
            )
            assertEquals(2, node.config.getOrNull(SemanticsActions.CustomActions)?.size)
            assertTrue(node.config.contains(SemanticsActions.OnClick))
            assertTrue(node.config.contains(SemanticsActions.OnLongClick))
        }
        assertTrue(description(may(7)).startsWith("5월 7일 목요일"))
        assertTrue(description(may(7)).contains("근무일"))
    }

    // --- 도우미 ---

    private fun cell(date: LocalDate): SemanticsNodeInteraction = rule.onNodeWithTag(cellTag(date))

    private fun cellExists(date: LocalDate): Boolean =
        rule.onAllNodes(hasTestTag(cellTag(date))).fetchSemanticsNodes().isNotEmpty()

    private fun description(date: LocalDate): String =
        cell(date).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)
            ?.joinToString().orEmpty()

    private fun bubbleHas(text: String): Boolean =
        rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private fun centerInGrid(date: LocalDate): Offset {
        val grid = rule.onNodeWithTag(CALENDAR_GRID_TAG).fetchSemanticsNode().boundsInRoot
        return cell(date).fetchSemanticsNode().boundsInRoot.center - grid.topLeft
    }

    // 200ms 넘게 누른 뒤 이동
    private fun drag(from: LocalDate, to: LocalDate) {
        val a = centerInGrid(from)
        val b = centerInGrid(to)
        rule.onNodeWithTag(CALENDAR_GRID_TAG).performTouchInput {
            down(a)
            advanceEventTime(LONG_PRESS_MS + 100)
            moveTo(Offset((a.x + b.x) / 2, (a.y + b.y) / 2))
            moveTo(b)
            up()
        }
    }

    private fun longPress(date: LocalDate) {
        cell(date).performTouchInput { longClick() }
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(hasTestTag(MEMO_FIELD_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        // 시트 열림 애니메이션이 끝난 뒤 누른다
        rule.waitForIdle()
    }

    private fun customAction(date: LocalDate, label: String) {
        val action = cell(date).fetchSemanticsNode().config[SemanticsActions.CustomActions].first {
            it.label ==
                label
        }
        rule.runOnIdle { action.action() }
    }

    private companion object {
        const val TIMEOUT = 5_000L
        const val MIN_TOUCH_DP = 48f
    }
}
