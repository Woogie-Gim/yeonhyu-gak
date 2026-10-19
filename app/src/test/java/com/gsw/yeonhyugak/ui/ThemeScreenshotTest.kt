package com.gsw.yeonhyugak.ui

import android.app.Application
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.feature.calendar.CALENDAR_SETTINGS_TAG
import com.gsw.yeonhyugak.feature.calendar.cellTag
import com.gsw.yeonhyugak.feature.settings.SETTINGS_TAG
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 달력 × 5테마 × 라이트/다크 × 글자 100·200%
// 기준 이미지는 사용자가 화면을 확인한 뒤 CI(screenshots.yml)로 기록한다
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class ThemeScreenshotTest(
    private val theme: ThemeId,
    private val dark: Boolean,
    private val fontScale: Float,
) {
    @get:Rule
    val rule = createComposeRule()

    private var harness: AppHarness? = null

    @After
    fun tearDown() {
        harness?.close()
    }

    private val name: String
        get() {
            val mode = if (dark) "dark" else "light"
            return "theme_${theme.name.lowercase()}_${mode}_${(fontScale * 100).toInt()}"
        }

    @Test
    fun calendar() {
        start(AppHarness.MAY_6)
        save("calendar_$name")
    }

    private fun start(today: LocalDate) {
        RuntimeEnvironment.setFontScale(fontScale)
        val h = AppHarness(today).apply {
            onboarded()
            setTheme(theme)
            sampleLeaves(today)
        }
        harness = h
        rule.setContent { h.App(dark) }
        waitFor(cellTag(today.withDayOfMonth(1)))
    }

    private fun waitFor(tag: String) = rule.waitUntil(TIMEOUT) {
        rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun save(file: String) {
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("src/test/screenshots/theme/$file.png")
    }

    companion object {
        private const val TIMEOUT = 10_000L

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_dark={1}_scale={2}")
        fun params(): List<Array<Any>> = ThemeId.entries.flatMap { t ->
            listOf(false, true).flatMap { d -> listOf(1f, 2f).map { s -> arrayOf<Any>(t, d, s) } }
        }

        // 연차 · 오전/오후 반차 · 메모 점 · 연휴 띠
        internal fun AppHarness.sampleLeaves(today: LocalDate) {
            mark(today.withDayOfMonth(4))
            mark(today.withDayOfMonth(19), type = LeaveType.AM_HALF)
            mark(today.withDayOfMonth(20), type = LeaveType.PM_HALF)
            runBlocking { memos.save(today.withDayOfMonth(14), "screenshot") }
        }
    }
}

// 계절 테마 12개월 + 테마 선택 화면
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class SeasonMonthScreenshotTest(private val month: Int) {
    @get:Rule
    val rule = createComposeRule()

    private var harness: AppHarness? = null

    @After
    fun tearDown() {
        harness?.close()
    }

    @Test
    fun seasonMonth() {
        RuntimeEnvironment.setFontScale(1f)
        val today = LocalDate.of(2026, month, 10)
        val h = AppHarness(today).apply {
            onboarded()
            setTheme(ThemeId.SEASON)
            with(ThemeScreenshotTest) { sampleLeaves(today) }
        }
        harness = h
        rule.setContent { h.App(false) }
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(
                hasTestTag(cellTag(today.withDayOfMonth(1))),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage(
            "src/test/screenshots/theme/season_month_%02d.png".format(month),
        )
    }

    companion object {
        private const val TIMEOUT = 10_000L

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "month={0}")
        fun params(): List<Array<Any>> = (1..12).map { arrayOf<Any>(it) }
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class SettingsScreenshotTest(private val dark: Boolean, private val fontScale: Float) {
    @get:Rule
    val rule = createComposeRule()

    private var harness: AppHarness? = null

    @After
    fun tearDown() {
        harness?.close()
    }

    @Test
    fun settings() {
        RuntimeEnvironment.setFontScale(fontScale)
        val h = AppHarness().apply { onboarded() }
        harness = h
        rule.setContent { h.App(dark) }
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(hasTestTag(CALENDAR_SETTINGS_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(CALENDAR_SETTINGS_TAG).performClick()
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(hasTestTag(SETTINGS_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
        val mode = if (dark) "dark" else "light"
        rule.onRoot().captureRoboImage(
            "src/test/screenshots/theme/settings_${mode}_${(fontScale * 100).toInt()}.png",
        )
    }

    companion object {
        private const val TIMEOUT = 10_000L

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}_scale={1}")
        fun params(): List<Array<Any>> =
            listOf(arrayOf(false, 1f), arrayOf(true, 1f), arrayOf(false, 2f))
    }
}
