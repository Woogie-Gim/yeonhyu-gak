package com.gsw.yeonhyugak.ui

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.engine.calendar.VacationSpan
import com.gsw.yeonhyugak.core.ui.STEPPER_MINUS_TAG
import com.gsw.yeonhyugak.core.ui.STEPPER_PLUS_TAG
import com.gsw.yeonhyugak.feature.calendar.CALENDAR_PREVIEW_ADD_TAG
import com.gsw.yeonhyugak.feature.calendar.CALENDAR_PREVIEW_SUMMARY_TAG
import com.gsw.yeonhyugak.feature.calendar.cellTag
import com.gsw.yeonhyugak.feature.leave.LEAVE_EDIT_SAVE_TAG
import com.gsw.yeonhyugak.feature.leave.LEAVE_EDIT_TAG
import com.gsw.yeonhyugak.feature.leave.LEAVE_REMAINING_TAG
import com.gsw.yeonhyugak.feature.recommend.REC_FREE_TITLE_TAG
import com.gsw.yeonhyugak.feature.recommend.REC_PERIOD_NEXT_TAG
import com.gsw.yeonhyugak.feature.recommend.REC_PERIOD_REST_TAG
import com.gsw.yeonhyugak.feature.recommend.REC_REMAINING_TAG
import com.gsw.yeonhyugak.feature.recommend.onboarding.ONBOARDING_START_TAG
import com.gsw.yeonhyugak.feature.recommend.onboarding.ONBOARDING_TAG
import com.gsw.yeonhyugak.feature.recommend.recCardTag
import com.gsw.yeonhyugak.feature.recommend.recCardTitleTag
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 첫 실행 → 추천 → 미리보기 → 담기 → 남은 연차 감소 → 되돌리기
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class AppFlowTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var harness: AppHarness

    @After
    fun tearDown() = harness.close()

    private fun launch(today: LocalDate = AppHarness.MAY_6, setup: AppHarness.() -> Unit = {}) {
        harness = AppHarness(today).apply(setup)
        rule.setContent { harness.App() }
    }

    @Test
    fun firstRun_showsOnboardingThenRecommend() {
        launch()
        waitFor(ONBOARDING_TAG)
        assertFalse(exists(tabTag(YhTab.CALENDAR)))

        rule.onNodeWithTag(STEPPER_PLUS_TAG).performClick()
        rule.onNodeWithTag(ONBOARDING_START_TAG).performClick()

        waitFor(REC_REMAINING_TAG)
        assertTrue(harness.onboardingDone())
        assertEquals(15.5, harness.allowance(), 0.0)
        rule.onNodeWithTag(REC_REMAINING_TAG).assert(hasText("남은 연차 15.5일"))
        rule.onNodeWithTag(tabTag(YhTab.RECOMMEND)).assertIsSelected()
        assertFalse(exists(ONBOARDING_TAG))
    }

    @Test
    fun onboardedUser_neverSeesOnboarding() {
        launch { onboarded() }
        waitFor(tabTag(YhTab.CALENDAR))
        rule.waitForIdle()
        assertFalse(exists(ONBOARDING_TAG))
    }

    // 추천 카드 일수 = 달력 미리보기 바 일수 = 띠 일수. 담은 뒤에도 같다
    @Test
    fun card_preview_add_undo_keepsDaysEqual() {
        launch()
        waitFor(ONBOARDING_START_TAG)
        rule.onNodeWithTag(ONBOARDING_START_TAG).performClick()
        waitFor(recCardTag(0))
        val title = text(recCardTitleTag(0))
        val (cost, days) = CARD_TITLE.find(title)!!.destructured

        // 첫 실행 3탭: 추천 보기 → 카드 → 달력에 담기
        rule.onNodeWithTag(recCardTag(0)).performClick()
        val dates = harness.preview.preview.value!!.leaveDates.sorted()
        assertEquals(cost.toInt(), dates.size)
        waitFor(cellTag(dates.first()))
        rule.onNodeWithTag(
            CALENDAR_PREVIEW_SUMMARY_TAG,
        ).assert(hasText("연차 ${cost}일 → ${days}일 휴가"))
        dates.forEach { assertTrue(description(it).contains("${days}일 연휴")) }

        rule.onNodeWithTag(CALENDAR_PREVIEW_ADD_TAG).performClick()
        rule.waitUntil(TIMEOUT) { harness.leaveMap().keys == dates.toSet() }
        // 스낵바는 저장 직후 이벤트로 뜬다
        rule.waitUntil(TIMEOUT) { textShown("달력에 담았어요") }
        assertTrue(description(dates.first()).contains("${days}일 연휴"))
        assertNull(harness.preview.preview.value)

        rule.onNodeWithText("되돌리기").performClick()
        rule.waitUntil(TIMEOUT) { harness.leaveMap().isEmpty() }
    }

    @Test
    fun add_decreasesRemaining_andRecommendUsesRest() {
        launch { onboarded(allowance = 15.0) }
        rule.onNodeWithTag(tabTag(YhTab.RECOMMEND)).performClick()
        waitFor(recCardTag(0))
        val cost = CARD_TITLE.find(text(recCardTitleTag(0)))!!.groupValues[1].toInt()
        rule.onNodeWithTag(recCardTag(0)).performClick()
        waitFor(CALENDAR_PREVIEW_ADD_TAG)
        rule.onNodeWithTag(CALENDAR_PREVIEW_ADD_TAG).performClick()
        rule.waitUntil(TIMEOUT) { harness.leaveMap().size == cost }

        rule.onNodeWithTag(tabTag(YhTab.LEAVE)).performClick()
        waitFor(LEAVE_REMAINING_TAG)
        rule.onNodeWithTag(LEAVE_REMAINING_TAG).assert(hasText("${15 - cost}일"))

        // 추천도 이미 쓴 만큼 빼고 계산
        rule.onNodeWithTag(tabTag(YhTab.RECOMMEND)).performClick()
        waitFor(REC_REMAINING_TAG)
        rule.onNodeWithTag(REC_REMAINING_TAG).assert(hasText("남은 연차 ${15 - cost}일"))
        rule.waitUntil(TIMEOUT) { harness.recommendState().items.isNotEmpty() }
        // 연도 경계 블록의 다음 해 연차는 따로 차감
        harness.recommendState().items.forEach {
            assertTrue((it.costByYear[2026] ?: 0) <= 15 - cost)
        }
    }

    @Test
    fun defaultFilter_isRestOfYear_withoutPastDays() {
        // 5/4(월) 연차가 있으면 5/1부터 이어지는 블록은 과거 포함이라 빠진다
        launch {
            onboarded()
            mark(LocalDate.of(2026, 5, 4))
        }
        rule.onNodeWithTag(tabTag(YhTab.RECOMMEND)).performClick()
        waitFor(REC_PERIOD_REST_TAG)
        rule.onNodeWithTag(REC_PERIOD_REST_TAG).assertIsSelected()
        rule.waitUntil(TIMEOUT) { harness.recommendState().items.isNotEmpty() }
        harness.recommendState().items.forEach {
            assertFalse(it.start.isBefore(AppHarness.MAY_6))
            assertEquals(2026, it.start.year)
        }
    }

    @Test
    fun nextYearChip_highlightedFromNovember() {
        launch(AppHarness.NOV_2) { onboarded() }
        rule.onNodeWithTag(tabTag(YhTab.RECOMMEND)).performClick()
        waitFor(REC_PERIOD_NEXT_TAG)
        assertEquals("추천", stateDescription(REC_PERIOD_NEXT_TAG))
        rule.onNodeWithTag(REC_PERIOD_REST_TAG).assertIsSelected()
    }

    @Test
    fun nextYearChip_notHighlightedInMay() {
        launch { onboarded() }
        rule.onNodeWithTag(tabTag(YhTab.RECOMMEND)).performClick()
        waitFor(REC_PERIOD_NEXT_TAG)
        assertNull(stateDescription(REC_PERIOD_NEXT_TAG))
    }

    @Test
    fun leave_editRemaining_thenUndo() {
        launch {
            onboarded(allowance = 15.0)
            mark(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2))
        }
        rule.onNodeWithTag(tabTag(YhTab.LEAVE)).performClick()
        waitFor(LEAVE_REMAINING_TAG)
        rule.onNodeWithTag(LEAVE_REMAINING_TAG).assert(hasText("13일"))

        rule.onNodeWithTag(LEAVE_EDIT_TAG).performClick()
        waitFor(LEAVE_EDIT_SAVE_TAG)
        rule.onNodeWithTag(STEPPER_MINUS_TAG).performClick()
        rule.onNodeWithTag(STEPPER_MINUS_TAG).performClick()
        rule.onNodeWithTag(LEAVE_EDIT_SAVE_TAG).performClick()
        // 시트 닫힘 애니메이션 동안 메인 루퍼를 돌린다
        rule.waitForIdle()
        rule.waitUntil(TIMEOUT) { harness.allowance() == 14.0 }
        rule.onNodeWithTag(LEAVE_REMAINING_TAG).assert(hasText("12일"))
        // 스낵바는 저장 직후 이벤트로 뜬다
        rule.waitUntil(TIMEOUT) { textShown("남은 연차를 고쳤어요") }

        rule.onNodeWithText("되돌리기").performClick()
        rule.waitForIdle()
        rule.waitUntil(TIMEOUT) { harness.allowance() == 15.0 }
    }

    @Test
    fun zeroRemaining_showsFreeBreaks() {
        launch { onboarded(allowance = 0.0) }
        rule.onNodeWithTag(tabTag(YhTab.RECOMMEND)).performClick()
        waitFor(REC_FREE_TITLE_TAG)
        waitFor(recCardTag(0))
        assertTrue(text(recCardTitleTag(0)).startsWith("연차 없이"))
        harness.recommendState().freeBreaks.forEach {
            assertTrue(it.totalDays >= VacationSpan.MIN_DAYS)
        }
    }

    // --- 도우미 ---

    private fun textShown(text: String): Boolean =
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun exists(tag: String): Boolean =
        rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(tag: String) = rule.waitUntil(TIMEOUT) { exists(tag) }

    private fun text(tag: String): String =
        rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty()

    private fun description(date: LocalDate): String =
        rule.onNodeWithTag(cellTag(date)).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()

    private fun stateDescription(tag: String): String? =
        rule.onNodeWithTag(tag).fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.StateDescription)

    private companion object {
        const val TIMEOUT = 10_000L
        val CARD_TITLE = Regex("""연차 (\d+)일 → (\d+)일 휴가""")
    }
}
