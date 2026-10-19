package com.gsw.yeonhyugak.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.engine.appearance.BaseballSeason
import com.gsw.yeonhyugak.core.ui.theme.BackdropKind
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// 계절 테마 입자 상한 (SPEC 7장)
const val MAX_PARTICLES = 20

enum class SeasonMotif { SNOW, PETAL, WAVE, LEAF, STAR }

// 1~2월 눈 · 3~5월 꽃잎 · 6~8월 물결 · 9~11월 낙엽 · 12월 별
fun seasonMotif(month: Int): SeasonMotif = MONTH_MOTIFS[(month - 1).coerceIn(MONTH_MOTIFS.indices)]

private val MONTH_MOTIFS = listOf(
    SeasonMotif.SNOW, SeasonMotif.SNOW,
    SeasonMotif.PETAL, SeasonMotif.PETAL, SeasonMotif.PETAL,
    SeasonMotif.WAVE, SeasonMotif.WAVE, SeasonMotif.WAVE,
    SeasonMotif.LEAF, SeasonMotif.LEAF, SeasonMotif.LEAF,
    SeasonMotif.STAR,
)

// 입자 자리: 달마다 같은 배치 (스크린샷 재현)
internal data class Particle(val x: Float, val y: Float, val size: Float, val phase: Float)

internal fun particlesOf(month: Int, count: Int = MAX_PARTICLES): List<Particle> {
    val rnd = Random(month)
    return List(count.coerceAtMost(MAX_PARTICLES)) {
        Particle(
            rnd.nextFloat(),
            rnd.nextFloat(),
            MIN_SIZE + rnd.nextFloat() * SIZE_SPREAD,
            rnd.nextFloat(),
        )
    }
}

// 월별 배경. 이미지 없이 Canvas로. 애니메이션 줄이기면 멈춘 그림
// 그림은 backdropInk 한 색만 써서 위 글자 대비를 지킨다 (장식 줄은 가장자리에만)
@Composable
fun MonthBackdrop(month: Int, modifier: Modifier = Modifier) {
    val kind = YhTheme.spec.backdrop
    if (kind == BackdropKind.NONE) return
    val colors = YhTheme.colors
    val reduceMotion = rememberReduceMotion()
    val progress = if (reduceMotion || kind != BackdropKind.SEASON) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberDrift()
    }
    val particles = remember(month) { particlesOf(month) }
    Canvas(modifier.clearAndSetSemantics { }) {
        val ink = colors.backdropInk
        when (kind) {
            BackdropKind.SEASON -> drawSeason(seasonMotif(month), particles, progress.value, ink)
            BackdropKind.GRID -> drawGrid(ink, colors.decor)
            BackdropKind.WINDOW -> drawWindow(month, ink)
            BackdropKind.SCOREBOARD -> drawScoreboard(BaseballSeason.isOn(month), ink, colors.decor)
            BackdropKind.NONE -> Unit
        }
    }
}

@Composable
private fun rememberDrift(): State<Float> =
    rememberInfiniteTransition(label = "drift").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(DRIFT_MS, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "drift",
    )

private fun DrawScope.drawSeason(
    motif: SeasonMotif,
    particles: List<Particle>,
    t: Float,
    ink: Color,
) {
    val unit = 6.dp.toPx()
    particles.forEach { p ->
        // 위에서 아래로 천천히. 물결·별은 제자리
        val fall = if (motif == SeasonMotif.WAVE || motif == SeasonMotif.STAR) 0f else t
        val y = ((p.y + fall) % 1f) * size.height
        val sway = sin((t + p.phase) * 2 * PI).toFloat() * unit
        val x = p.x * size.width + if (motif == SeasonMotif.STAR) 0f else sway
        val r = unit * p.size
        when (motif) {
            SeasonMotif.SNOW -> drawCircle(ink, r * SNOW_SCALE, Offset(x, y))

            SeasonMotif.PETAL -> rotate(p.phase * FULL_TURN, Offset(x, y)) {
                drawOval(ink, Offset(x - r, y - r / 2), Size(r * 2, r))
            }

            SeasonMotif.WAVE -> drawWave(Offset(x, y), r * 2, ink)

            SeasonMotif.LEAF -> rotate(p.phase * FULL_TURN + t * QUARTER_TURN, Offset(x, y)) {
                drawPath(leafPath(Offset(x, y), r), ink)
            }

            SeasonMotif.STAR -> drawStar(Offset(x, y), r * STAR_SCALE, ink)
        }
    }
}

private fun DrawScope.drawWave(center: Offset, width: Float, ink: Color) {
    val path = Path().apply {
        moveTo(center.x - width, center.y)
        quadraticTo(center.x - width / 2, center.y - width / 3, center.x, center.y)
        quadraticTo(center.x + width / 2, center.y + width / 3, center.x + width, center.y)
    }
    drawPath(path, ink, style = Stroke(width = 2.dp.toPx()))
}

private fun leafPath(c: Offset, r: Float) = Path().apply {
    moveTo(c.x - r, c.y)
    quadraticTo(c.x, c.y - r, c.x + r, c.y)
    quadraticTo(c.x, c.y + r, c.x - r, c.y)
    close()
}

private fun DrawScope.drawStar(c: Offset, r: Float, ink: Color) {
    drawLine(ink, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = 1.5.dp.toPx())
    drawLine(ink, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = 1.5.dp.toPx())
}

// 아날로그: 모눈 + 왼쪽 제본 줄(표지 색)
private fun DrawScope.drawGrid(ink: Color, cover: Color) {
    val step = 16.dp.toPx()
    val stroke = 1.dp.toPx()
    var x = step
    while (x < size.width) {
        drawLine(ink, Offset(x, 0f), Offset(x, size.height), stroke)
        x += step
    }
    var y = step
    while (y < size.height) {
        drawLine(ink, Offset(0f, y), Offset(size.width, y), stroke)
        y += step
    }
    drawRect(cover, size = Size(3.dp.toPx(), size.height))
}

// Lo-fi: 오른쪽 아래 창. 창밖은 계절 따라 눈·비·달
private fun DrawScope.drawWindow(month: Int, ink: Color) {
    val w = size.width * 0.32f
    val h = size.height * 0.24f
    val left = size.width - w - 12.dp.toPx()
    val top = size.height - h - 12.dp.toPx()
    val stroke = Stroke(width = 2.dp.toPx())
    drawRect(ink, Offset(left, top), Size(w, h), style = stroke)
    drawLine(ink, Offset(left + w / 2, top), Offset(left + w / 2, top + h), 2.dp.toPx())
    drawLine(ink, Offset(left, top + h / 2), Offset(left + w, top + h / 2), 2.dp.toPx())
    particlesOf(month, WINDOW_PARTICLES).forEach { p ->
        val c = Offset(left + p.x * w, top + p.y * h)
        when (seasonMotif(month)) {
            SeasonMotif.SNOW, SeasonMotif.STAR -> drawCircle(ink, 1.5.dp.toPx(), c)
            SeasonMotif.WAVE -> drawLine(ink, c, c + Offset(-2.dp.toPx(), 6.dp.toPx()), 1.dp.toPx())
            SeasonMotif.PETAL, SeasonMotif.LEAF -> drawCircle(ink, 2.dp.toPx(), c)
        }
    }
    // 달
    drawCircle(ink, h * MOON_SIZE, Offset(left + w * MOON_X, top + h * MOON_Y))
}

// 야구: 전광판 점. 비시즌은 점을 듬성듬성(조명 꺼짐). 위아래 응원 색 줄
private fun DrawScope.drawScoreboard(lit: Boolean, ink: Color, cheer: Color) {
    val step = if (lit) 10.dp.toPx() else 20.dp.toPx()
    val r = if (lit) 1.5.dp.toPx() else 1.dp.toPx()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            drawCircle(ink, r, Offset(x, y))
            x += step
        }
        y += step
    }
    val stripe = 3.dp.toPx()
    drawRect(cheer, size = Size(size.width, stripe))
    drawRect(cheer, Offset(0f, size.height - stripe), Size(size.width, stripe))
}

private const val DRIFT_MS = 24_000
private const val MIN_SIZE = 0.6f
private const val SIZE_SPREAD = 0.8f
private const val SNOW_SCALE = 0.6f
private const val STAR_SCALE = 0.7f
private const val FULL_TURN = 360f
private const val QUARTER_TURN = 90f
private const val MOON_SIZE = 0.14f
private const val MOON_X = 0.78f
private const val MOON_Y = 0.25f
private const val WINDOW_PARTICLES = 8
