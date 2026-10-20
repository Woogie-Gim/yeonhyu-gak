package com.gsw.yeonhyugak.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.feature.calendar.CALENDAR_PREVIEW_ADD_TAG
import com.gsw.yeonhyugak.feature.calendar.CALENDAR_SETTINGS_TAG
import com.gsw.yeonhyugak.feature.calendar.CALENDAR_SHARE_TAG
import com.gsw.yeonhyugak.feature.calendar.cellTag
import com.gsw.yeonhyugak.feature.recommend.onboarding.ONBOARDING_START_TAG
import com.gsw.yeonhyugak.feature.recommend.recCardTag
import com.gsw.yeonhyugak.feature.settings.SETTINGS_DEV_TAG
import com.gsw.yeonhyugak.feature.settings.SETTINGS_NOTIFY_TAG
import com.gsw.yeonhyugak.feature.settings.SETTINGS_QR_TAG
import com.gsw.yeonhyugak.feature.settings.SETTINGS_TAG
import com.gsw.yeonhyugak.feature.share.SHARE_CARD_TAG
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 알림 권한 시점 · 위젯/알림에서 열기 · 공유 진입 · 설정 토글
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class AlertsShareFlowTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var harness: AppHarness

    @After
    fun tearDown() {
        if (::harness.isInitialized) harness.close()
    }

    private fun launch(setup: AppHarness.() -> Unit = {}) {
        harness = AppHarness().apply(setup)
        rule.setContent { harness.App() }
    }

    // 첫 실행 흐름(온보딩 → 추천 → 카드 → 미리보기)에서는 묻지 않고 담은 직후 한 번만
    @Test
    fun firstRun_neverAsksPermission_untilFirstAdd() {
        launch()
        waitFor(ONBOARDING_START_TAG)
        assertNoAsk()
        rule.onNodeWithTag(ONBOARDING_START_TAG).performClick()
        waitFor(recCardTag(0))
        assertNoAsk()
        rule.onNodeWithTag(recCardTag(0)).performClick()
        waitFor(CALENDAR_PREVIEW_ADD_TAG)
        assertNoAsk()

        rule.onNodeWithTag(CALENDAR_PREVIEW_ADD_TAG).performClick()
        rule.waitUntil(TIMEOUT) { textShown(ASK) }
        assertEquals(0, harness.permissionRequests)
        rule.waitForIdle()
        rule.onNodeWithText("알려주세요").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(1, harness.permissionRequests)
        awaitPref { harness.alertPrefs.notifyOn.first() }
    }

    @Test
    fun manifest_noExactAlarmPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toList()
        assertTrue(requested.contains(Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(requested.contains(Manifest.permission.INTERNET))
        assertFalse(requested.contains(Manifest.permission.SCHEDULE_EXACT_ALARM))
        assertFalse(requested.contains(Manifest.permission.USE_EXACT_ALARM))
    }

    @Test
    fun openDate_fromWidget_switchesToCalendarMonth() {
        launch { onboarded() }
        waitFor(tabTag(YhTab.LEAVE))
        rule.onNodeWithTag(tabTag(YhTab.LEAVE)).performClick()
        rule.waitForIdle()

        val target = LocalDate.of(2026, 9, 15)
        harness.openDates.open(target)
        waitFor(cellTag(target))
        rule.onNodeWithTag(tabTag(YhTab.CALENDAR)).assertIsSelected()
        rule.waitUntil(TIMEOUT) { harness.openDates.date.value == null }
    }

    @Test
    fun shareIcon_opensCardOfShownYear() {
        launch { onboarded() }
        waitFor(CALENDAR_SHARE_TAG)
        rule.onNodeWithTag(CALENDAR_SHARE_TAG).performClick()
        waitFor(SHARE_CARD_TAG)
        assertTrue(textShown("2026년 연휴"))
    }

    @Test
    fun settings_qrAndNotifyToggles() {
        launch { onboarded() }
        waitFor(CALENDAR_SETTINGS_TAG)
        rule.onNodeWithTag(CALENDAR_SETTINGS_TAG).performClick()
        scrollTo(SETTINGS_QR_TAG)
        click(SETTINGS_QR_TAG)
        awaitPref { !harness.prefs.shareQr.first() }

        scrollTo(SETTINGS_NOTIFY_TAG)
        click(SETTINGS_NOTIFY_TAG)
        assertEquals(1, harness.permissionRequests)
        awaitPref { harness.alertPrefs.notifyOn.first() }
        // 스위치가 켜진 모습으로 다시 그려진 뒤에 끈다
        rule.waitUntil(TIMEOUT) { toggleState(SETTINGS_NOTIFY_TAG) == ToggleableState.On }

        click(SETTINGS_NOTIFY_TAG)
        awaitPref { !harness.alertPrefs.notifyOn.first() }
        // 끌 때는 권한을 묻지 않는다
        assertEquals(1, harness.permissionRequests)
    }

    @Test
    fun settings_deniedPermission_staysOff() {
        launch {
            onboarded()
            grantPermission = false
        }
        waitFor(CALENDAR_SETTINGS_TAG)
        rule.onNodeWithTag(CALENDAR_SETTINGS_TAG).performClick()
        scrollTo(SETTINGS_NOTIFY_TAG)
        click(SETTINGS_NOTIFY_TAG)
        rule.waitUntil(TIMEOUT) { textShown("알림 권한이 꺼져 있어요. 기기 설정에서 켜 주세요") }
        assertFalse(runBlocking { harness.alertPrefs.notifyOn.first() })
    }

    // 기본 하네스(devMenu = null)는 릴리스와 같은 구성: 개발자 메뉴 줄 없음
    @Test
    fun settings_withoutDevMenu_hasNoDevRow() {
        launch { onboarded() }
        waitFor(CALENDAR_SETTINGS_TAG)
        rule.onNodeWithTag(CALENDAR_SETTINGS_TAG).performClick()
        scrollTo(SETTINGS_QR_TAG)
        // 목록 끝까지 내려도 개발자 메뉴 줄이 없다
        rule.onNodeWithTag(SETTINGS_TAG).performScrollToIndex(lastIndex())
        rule.waitForIdle()
        assertFalse(exists(SETTINGS_DEV_TAG))
    }

    private fun click(tag: String) {
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
    }

    // 설정은 LazyColumn이라 보이는 줄만 만들어진다
    private fun scrollTo(tag: String) {
        waitFor(SETTINGS_TAG)
        rule.onNodeWithTag(SETTINGS_TAG).performScrollToNode(hasTestTag(tag))
        waitFor(tag)
    }

    private fun lastIndex(): Int = rule.onNodeWithTag(SETTINGS_TAG).fetchSemanticsNode()
        .config[SemanticsProperties.CollectionInfo].rowCount - 1

    private fun toggleState(tag: String): ToggleableState? =
        rule.onNodeWithTag(tag).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.ToggleableState)

    private fun assertNoAsk() {
        rule.waitForIdle()
        assertFalse(textShown(ASK))
        assertEquals(0, harness.permissionRequests)
    }

    // DataStore 쓰기: 메인 루퍼를 비우고 IO 스레드가 끝낼 시간을 준다
    private fun awaitPref(read: suspend () -> Boolean) {
        val until = System.currentTimeMillis() + TIMEOUT
        while (true) {
            rule.waitForIdle()
            if (runBlocking { read() }) return
            check(System.currentTimeMillis() < until) { "설정 값이 바뀌지 않음" }
            Thread.sleep(POLL_MS)
        }
    }

    private fun textShown(text: String): Boolean =
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun exists(tag: String): Boolean =
        rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(tag: String) = rule.waitUntil(TIMEOUT) { exists(tag) }

    private companion object {
        const val TIMEOUT = 5_000L
        const val POLL_MS = 50L
        const val ASK = "연휴 3주 전에 알려드릴까요?"
    }
}
