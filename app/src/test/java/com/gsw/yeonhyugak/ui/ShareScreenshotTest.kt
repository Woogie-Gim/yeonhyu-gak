package com.gsw.yeonhyugak.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.github.takahirom.roborazzi.captureRoboImage
import com.gsw.yeonhyugak.core.engine.appearance.Appearance
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.holiday.HolidaySource
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.feature.share.SHARE_ACTION_TAG
import com.gsw.yeonhyugak.feature.share.SHARE_CARD_TAG
import com.gsw.yeonhyugak.feature.share.ShareBlock
import com.gsw.yeonhyugak.feature.share.ShareCard
import com.gsw.yeonhyugak.feature.share.ShareCardData
import com.gsw.yeonhyugak.feature.share.ShareRatio
import com.gsw.yeonhyugak.feature.share.ShareSheetContent
import com.gsw.yeonhyugak.feature.share.ratioTag
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 공유 카드 × 5테마 × 비율 2. 기준 이미지는 CI(screenshots.yml)로 기록
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class ShareScreenshotTest(private val theme: ThemeId, private val ratio: ShareRatio) {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun Themed(content: @Composable () -> Unit) {
        YhTheme(
            darkTheme = false,
            appearance = Appearance(theme = theme),
            month = 5,
            content = content,
        )
    }

    @Test
    fun card() {
        rule.setContent { Themed { Box { ShareCard(SAMPLE, ratio, "연휴각") } } }
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(hasTestTag(SHARE_CARD_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(SHARE_CARD_TAG).captureRoboImage(
            "src/test/screenshots/share/share_${theme.name.lowercase()}_${ratio.name.lowercase()}.png",
        )
    }

    // 공유하기 → 카드 비율 그대로의 이미지 (기본 노트에서만)
    @Test
    fun shareButton_exportsCardImage() {
        if (theme != ThemeId.NOTE) return
        val shared = mutableListOf<Bitmap>()
        rule.setContent { Themed { ShareSheetContent(SAMPLE) { _, b -> shared += b } } }
        rule.onNodeWithTag(ratioTag(ratio)).performClick()
        rule.waitForIdle()
        // Robolectric은 화면을 그리지 않는다. 실기기처럼 한 번 그려 카드를 기록
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val frame = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(frame))
        }
        rule.onNodeWithTag(SHARE_ACTION_TAG).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(TIMEOUT) {
            check(rule.onAllNodesWithText(FAILED).fetchSemanticsNodes().isEmpty()) {
                "카드 이미지 생성 실패"
            }
            shared.isNotEmpty()
        }
        val image = shared.single()
        assertEquals(
            ratio.height.toDouble() / ratio.width,
            image.height.toDouble() / image.width,
            RATIO_TOLERANCE,
        )
    }

    companion object {
        private const val TIMEOUT = 5_000L
        private const val RATIO_TOLERANCE = 0.02
        private const val FAILED = "카드를 만들지 못했어요. 다시 눌러 주세요"

        private fun holiday(
            date: LocalDate,
            name: String,
            kind: HolidayKind = HolidayKind.NATIONAL,
        ) = Holiday(date, name, kind, confirmed = true, source = HolidaySource.BUNDLED)

        private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)

        val SAMPLE = ShareCardData(
            year = 2026,
            blocks = listOf(
                ShareBlock(d(2, 14), d(2, 18), 5, holiday(d(2, 17), "설날", HolidayKind.LUNAR)),
                ShareBlock(d(5, 1), d(5, 5), 5, holiday(d(5, 1), "노동절")),
                ShareBlock(d(5, 23), d(5, 25), 3, holiday(d(5, 24), "부처님오신날", HolidayKind.LUNAR)),
                ShareBlock(d(8, 14), d(8, 17), 4, holiday(d(8, 15), "광복절")),
                ShareBlock(d(9, 24), d(9, 28), 5, holiday(d(9, 25), "추석", HolidayKind.LUNAR)),
                ShareBlock(d(10, 3), d(10, 11), 9, holiday(d(10, 3), "개천절")),
                ShareBlock(d(12, 25), d(12, 27), 3, holiday(d(12, 25), "기독탄신일")),
            ),
            totalDays = 34,
            usedLeave = 6.5,
            expected = false,
            showQr = true,
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun params(): List<Array<Any>> =
            ThemeId.entries.flatMap { t -> ShareRatio.entries.map { arrayOf<Any>(t, it) } }
    }
}
