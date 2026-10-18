package com.gsw.yeonhyugak.ui

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.gsw.yeonhyugak.ui.theme.YhTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 빈 앱 골격 스크린샷. 라이트·다크
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h640dp-xhdpi")
class AppShellScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appShell_light() = capture(darkTheme = false, name = "app_shell_light")

    @Test
    fun appShell_dark() = capture(darkTheme = true, name = "app_shell_dark")

    private fun capture(darkTheme: Boolean, name: String) {
        composeRule.setContent {
            YhTheme(darkTheme = darkTheme) { YhApp() }
        }
        composeRule.onNodeWithTag(APP_ROOT_TAG).assertExists()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
