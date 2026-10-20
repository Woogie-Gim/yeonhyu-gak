package com.gsw.yeonhyugak.feature.calendar

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.ui.LocalNotificationPermission
import com.gsw.yeonhyugak.core.ui.NotificationPermissionRequester
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.feature.calendar.CalendarHarness.Companion.REMOTE_ELECTION
import com.gsw.yeonhyugak.feature.calendar.CalendarHarness.Companion.TODAY
import com.gsw.yeonhyugak.feature.calendar.CalendarHarness.Companion.may
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 알림 권한은 첫 담기 직후에만 · 위젯/알림에서 연 날짜 · 새 공휴일 N 배지 · 공유 진입
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class CalendarAlertsTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var harness: CalendarHarness
    private var permissionRequests = 0
    private var grant = true
    private val shared = mutableListOf<Int>()

    @Before
    fun setUp() {
        harness = CalendarHarness()
        runBlocking { harness.alertPrefs.addNewHolidays(listOf(REMOTE_ELECTION.date), TODAY) }
        val requester = NotificationPermissionRequester { done ->
            permissionRequests++
            done(grant)
        }
        rule.setContent {
            CompositionLocalProvider(LocalNotificationPermission provides requester) {
                YhTheme(darkTheme = false) {
                    val host = remember { SnackbarHostState() }
                    Box(Modifier.fillMaxSize()) {
                        CalendarRoute(
                            snackbarHostState = host,
                            onOpenShare = { shared += it },
                            viewModel = harness.viewModel(),
                        )
                        SnackbarHost(host, Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
        rule.waitUntil(TIMEOUT) { cellExists(may(1)) }
    }

    @After
    fun tearDown() = harness.close()

    @Test
    fun tapAndDrag_neverAskPermission() {
        rule.onNodeWithTag(cellTag(may(7))).performClick()
        rule.waitUntil(TIMEOUT) { harness.leaveMap().isNotEmpty() }
        rule.waitForIdle()
        assertFalse(dialogShown())
        assertEquals(0, permissionRequests)
    }

    @Test
    fun firstAdd_asksOnce_yesRequestsPermission() {
        add(may(7))
        rule.waitUntil(TIMEOUT) { dialogShown() }
        tapButton("알려주세요")
        rule.waitUntil(TIMEOUT) { !dialogShown() }
        assertEquals(1, permissionRequests)
        awaitPref { harness.alertPrefs.notifyOn.first() }

        // 두 번째 담기는 묻지 않는다
        add(may(20))
        rule.waitUntil(TIMEOUT) { harness.leaveMap().size == 2 }
        rule.waitForIdle()
        assertFalse(dialogShown())
        assertEquals(1, permissionRequests)
    }

    @Test
    fun deniedPermission_keepsNotificationsOff() {
        grant = false
        add(may(7))
        rule.waitUntil(TIMEOUT) { dialogShown() }
        tapButton("알려주세요")
        awaitPref { harness.alertPrefs.notifyAsked.first() }
        assertFalse(runBlocking { harness.alertPrefs.notifyOn.first() })
    }

    @Test
    fun no_skipsSystemPermission() {
        add(may(7))
        rule.waitUntil(TIMEOUT) { dialogShown() }
        tapButton("괜찮아요")
        awaitPref { harness.alertPrefs.notifyAsked.first() }
        assertEquals(0, permissionRequests)
        assertFalse(runBlocking { harness.alertPrefs.notifyOn.first() })
    }

    @Test
    fun openDate_movesToThatMonth() {
        harness.openDates.open(LocalDate.of(2026, 9, 15))
        rule.waitUntil(TIMEOUT) { cellExists(LocalDate.of(2026, 9, 15)) }
        rule.onNodeWithContentDescription("2026년 9월").assertExists()
        rule.waitUntil(TIMEOUT) { harness.openDates.date.value == null }
        assertNull(harness.openDates.date.value)
    }

    @Test
    fun newRemoteHoliday_hasNBadgeAndDescription() {
        assertTrue(description(REMOTE_ELECTION.date).contains("새로 추가된 공휴일"))
        assertFalse(description(may(12)).contains("새로 추가된 공휴일"))
    }

    @Test
    fun shareIcon_opensCardForShownYear() {
        rule.onNodeWithContentDescription("공유 카드 만들기").performClick()
        assertEquals(listOf(2026), shared)
    }

    private fun add(date: LocalDate) {
        harness.preview.show(listOf(date))
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(hasTestTag(CALENDAR_PREVIEW_ADD_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        // 스낵바가 버튼을 덮어도 누를 수 있게 시맨틱 동작으로
        rule.onNodeWithTag(
            CALENDAR_PREVIEW_ADD_TAG,
        ).performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun tapButton(text: String) {
        rule.waitForIdle()
        rule.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
    }

    // DataStore 쓰기는 메인 루퍼가 돌아야 끝난다
    private fun awaitPref(read: suspend () -> Boolean) {
        rule.waitUntil(TIMEOUT) {
            rule.mainClock.advanceTimeBy(FRAME_MS)
            runBlocking { read() }
        }
    }

    private fun dialogShown(): Boolean =
        rule.onAllNodesWithText("연휴 3주 전에 알려드릴까요?").fetchSemanticsNodes().isNotEmpty()

    private fun cellExists(date: LocalDate): Boolean =
        rule.onAllNodes(hasTestTag(cellTag(date))).fetchSemanticsNodes().isNotEmpty()

    private fun description(date: LocalDate): String =
        rule.onNodeWithTag(cellTag(date)).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.ContentDescription)
            ?.joinToString().orEmpty()

    private companion object {
        const val TIMEOUT = 5_000L
        const val FRAME_MS = 16L
    }
}
