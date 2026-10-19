package com.gsw.yeonhyugak.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.feature.calendar.cellTag
import com.gsw.yeonhyugak.ui.ThemeScreenshotTest.Companion.sampleLeaves
import java.io.File
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 색각 이상 시뮬레이션 확인용 이미지 (기준 이미지 아님). build/outputs/colorblind/
// 공휴일 = 이름 글자 · 연차 = 칸 채움 · 반차 = 반쪽 + 진한 변으로 색 없이도 구분되는지 사람이 본다
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "w360dp-h740dp-xhdpi")
class ColorBlindScreenshotTest(private val theme: ThemeId) {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var harness: AppHarness? = null

    @After
    fun tearDown() {
        harness?.close()
    }

    @Test
    fun simulate() {
        RuntimeEnvironment.setFontScale(1f)
        val today = AppHarness.MAY_6
        val h = AppHarness(today).apply {
            onboarded()
            setTheme(theme)
            sampleLeaves(today)
        }
        harness = h
        rule.setContent { h.App(false) }
        rule.waitUntil(TIMEOUT) {
            rule.onAllNodes(
                hasTestTag(cellTag(today.withDayOfMonth(1))),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
        // captureToImage는 Robolectric에서 동작하지 않아 화면 View를 직접 그린다
        val view = rule.activity.window.decorView
        val source = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(source))
        val dir = File("build/outputs/colorblind").apply { mkdirs() }
        val base = theme.name.lowercase()
        write(source, File(dir, "${base}_normal.png"))
        SIMULATIONS.forEach { (kind, matrix) ->
            val out = File(dir, "${base}_$kind.png")
            write(apply(source, matrix), out)
            assertTrue(out.length() > 0)
        }
    }

    private fun apply(src: Bitmap, m: FloatArray): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16 and 0xFF).toFloat()
            val g = (p shr 8 and 0xFF).toFloat()
            val b = (p and 0xFF).toFloat()
            val nr = (m[0] * r + m[1] * g + m[2] * b).toInt().coerceIn(0, 255)
            val ng = (m[3] * r + m[4] * g + m[5] * b).toInt().coerceIn(0, 255)
            val nb = (m[6] * r + m[7] * g + m[8] * b).toInt().coerceIn(0, 255)
            pixels[i] = (p and ALPHA_MASK) or (nr shl 16) or (ng shl 8) or nb
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun write(bitmap: Bitmap, file: File) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
    }

    companion object {
        private const val TIMEOUT = 10_000L
        private const val ALPHA_MASK = 0xFF000000.toInt()
        private const val PNG_QUALITY = 100

        // Machado 2009 심각도 1.0 (sRGB 근사)
        private val SIMULATIONS = mapOf(
            "protanopia" to floatArrayOf(
                0.152286f, 1.052583f, -0.204868f,
                0.114503f, 0.786281f, 0.099216f,
                -0.003882f, -0.048116f, 1.051998f,
            ),
            "deuteranopia" to floatArrayOf(
                0.367322f, 0.860646f, -0.227968f,
                0.280085f, 0.672501f, 0.047413f,
                -0.011820f, 0.042940f, 0.968881f,
            ),
            "tritanopia" to floatArrayOf(
                1.255528f, -0.076749f, -0.178779f,
                -0.078411f, 0.930809f, 0.147602f,
                0.004733f, 0.691367f, 0.303900f,
            ),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun params(): List<Array<Any>> = ThemeId.entries.map { arrayOf<Any>(it) }
    }
}
