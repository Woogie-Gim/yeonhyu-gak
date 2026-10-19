package com.gsw.yeonhyugak.ui

import android.app.Application
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.feature.calendar.cellTag
import com.gsw.yeonhyugak.feature.leave.LEAVE_REMAINING_TAG
import com.gsw.yeonhyugak.feature.recommend.REC_FREE_TITLE_TAG
import com.gsw.yeonhyugak.feature.recommend.onboarding.ONBOARDING_TAG
import com.gsw.yeonhyugak.feature.recommend.recCardTag
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 달력 · 온보딩 · 추천 · 내 연차 · 빈 상태 × 라이트·다크 × 글자 100%·200%
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class CalendarScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private var harness: AppHarness? = null

    @After
    fun tearDown() {
        harness?.close()
    }

    @Test
    fun calendar_light_100() = calendar("calendar_light_100", dark = false, fontScale = 1f)

    @Test
    fun calendar_dark_100() = calendar("calendar_dark_100", dark = true, fontScale = 1f)

    @Test
    fun calendar_light_200() = calendar("calendar_light_200", dark = false, fontScale = 2f)

    @Test
    fun calendar_dark_200() = calendar("calendar_dark_200", dark = true, fontScale = 2f)

    // 예상 공휴일 점선 + 예상 배지 + 첫 진입 코치마크
    @Test
    fun calendar_expectedYear() = calendar(
        "calendar_expected_2030",
        dark = false,
        fontScale = 1f,
        today = LocalDate.of(2030, 9, 10),
        coachShown = false,
    )

    // 추천 카드에서 온 미리보기: 빗금 칸 + 하단 담기 바
    @Test
    fun calendar_preview() {
        val h = start(dark = false, fontScale = 1f, today = MAY_6) {
            onboarded()
            preview.show(listOf(LocalDate.of(2026, 5, 7), LocalDate.of(2026, 5, 8)))
        }
        h.waitForCell(LocalDate.of(2026, 5, 1))
        save("calendar_preview_light")
    }

    @Test
    fun onboarding_light_100() = onboarding("onboarding_light_100", dark = false, fontScale = 1f)

    @Test
    fun onboarding_dark_200() = onboarding("onboarding_dark_200", dark = true, fontScale = 2f)

    @Test
    fun recommend_light_100() = recommend("recommend_light_100", dark = false, fontScale = 1f)

    @Test
    fun recommend_dark_100() = recommend("recommend_dark_100", dark = true, fontScale = 1f)

    @Test
    fun recommend_light_200() = recommend("recommend_light_200", dark = false, fontScale = 2f)

    // 11월: "내년 보기" 칩 강조
    @Test
    fun recommend_november() =
        recommend("recommend_november_light", dark = false, fontScale = 1f, today = NOV_2)

    // 남은 연차 0: 연차 없이 쉬는 날
    @Test
    fun recommend_zeroLeft() {
        start(dark = false, fontScale = 1f, today = MAY_6) { onboarded(allowance = 0.0) }
        rule.onNodeWithTag(tabTag(YhTab.RECOMMEND)).performClick()
        waitFor(REC_FREE_TITLE_TAG)
        save("recommend_zero_light")
    }

    @Test
    fun leave_light_100() = leave("leave_light_100", dark = false, fontScale = 1f)

    @Test
    fun leave_dark_100() = leave("leave_dark_100", dark = true, fontScale = 1f)

    @Test
    fun leave_light_200() = leave("leave_light_200", dark = false, fontScale = 2f)

    // 지정 기록이 없을 때 빈 상태
    @Test
    fun emptyTab_leave() {
        start(dark = false, fontScale = 1f, today = MAY_6) { onboarded() }
        rule.onNodeWithTag(tabTag(YhTab.LEAVE)).performClick()
        waitFor(LEAVE_REMAINING_TAG)
        save("empty_leave_light")
    }

    // --- 도우미 ---

    private fun calendar(
        name: String,
        dark: Boolean,
        fontScale: Float,
        today: LocalDate = MAY_6,
        coachShown: Boolean = true,
    ) {
        val h = start(dark, fontScale, today) {
            onboarded(coachShown = coachShown)
            sampleLeaves(today)
        }
        h.waitForCell(today.withDayOfMonth(1))
        save(name)
    }

    private fun onboarding(name: String, dark: Boolean, fontScale: Float) {
        start(dark, fontScale, MAY_6) {}
        waitFor(ONBOARDING_TAG)
        save(name)
    }

    private fun recommend(name: String, dark: Boolean, fontScale: Float, today: LocalDate = MAY_6) {
        start(dark, fontScale, today) {
            onboarded()
            // 기존 연차와 붙은 카드도 보이게
            mark(LocalDate.of(2026, 6, 5))
        }
        rule.onNodeWithTag(tabTag(YhTab.RECOMMEND)).performClick()
        waitFor(recCardTag(0))
        save(name)
    }

    private fun leave(name: String, dark: Boolean, fontScale: Float) {
        start(dark, fontScale, MAY_6) {
            onboarded()
            sampleLeaves(MAY_6)
        }
        rule.onNodeWithTag(tabTag(YhTab.LEAVE)).performClick()
        waitFor(LEAVE_REMAINING_TAG)
        save(name)
    }

    // 연차 · 오전/오후 반차 · 메모 점 · 연휴 띠가 모두 보이게
    private fun AppHarness.sampleLeaves(today: LocalDate) {
        mark(today.withDayOfMonth(4))
        mark(today.withDayOfMonth(19), type = LeaveType.AM_HALF)
        mark(today.withDayOfMonth(20), type = LeaveType.PM_HALF)
        runBlocking { memos.save(today.withDayOfMonth(14), "screenshot") }
    }

    private fun start(
        dark: Boolean,
        fontScale: Float,
        today: LocalDate,
        setup: AppHarness.() -> Unit,
    ): AppHarness {
        RuntimeEnvironment.setFontScale(fontScale)
        val h = AppHarness(today).apply(setup)
        harness = h
        rule.setContent { h.App(dark) }
        return h
    }

    private fun AppHarness.waitForCell(date: LocalDate) = waitFor(cellTag(date))

    private fun waitFor(tag: String) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun save(name: String) {
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private companion object {
        val MAY_6: LocalDate = AppHarness.MAY_6
        val NOV_2: LocalDate = AppHarness.NOV_2
        const val TIMEOUT = 10_000L
    }
}
