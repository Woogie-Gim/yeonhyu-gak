package com.gsw.yeonhyugak.core.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.ui.theme.MonthTransition
import kotlin.math.abs
import kotlin.random.Random

// 애니메이션 줄이기면 100ms 크로스페이드
const val REDUCED_TRANSITION_MS = 100

// 페이저 페이지별 테마 전환 효과. offset 0 = 화면 가운데
fun Modifier.monthTransition(
    pagerState: PagerState,
    page: Int,
    transition: MonthTransition,
    reduceMotion: Boolean,
    grain: Color,
): Modifier {
    fun offset(): Float = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    if (reduceMotion) {
        // 제자리 크로스페이드: 미끄러짐을 상쇄하고 투명도만
        return graphicsLayer {
            val o = offset()
            translationX = o * size.width
            alpha = 1f - abs(o).coerceIn(0f, 1f)
        }
    }
    return when (transition) {
        MonthTransition.SLIDE -> this

        MonthTransition.FADE_SLIDE -> graphicsLayer {
            alpha = 1f - abs(offset()).coerceIn(0f, 1f) * FADE
        }

        // 종이 넘김: 넘어가는 쪽이 살짝 작아지며 그림자
        MonthTransition.PAPER -> graphicsLayer {
            val o = abs(offset()).coerceIn(0f, 1f)
            scaleX = 1f - o * PAPER_SCALE
            scaleY = 1f - o * PAPER_SCALE
            shadowElevation = o * 12.dp.toPx()
        }

        // 그레인 낀 페이드
        MonthTransition.GRAIN_FADE -> graphicsLayer {
            alpha = 1f - abs(offset()).coerceIn(0f, 1f) * FADE
        }.drawWithContent {
            drawContent()
            val o = abs(offset()).coerceIn(0f, 1f)
            if (o > 0f) {
                val rnd = Random(page)
                repeat(GRAIN_DOTS) {
                    drawCircle(
                        grain.copy(alpha = o),
                        radius = 1.dp.toPx(),
                        center = Offset(
                            rnd.nextFloat() * size.width,
                            rnd.nextFloat() * size.height,
                        ),
                    )
                }
            }
        }

        // 숫자 플립: 제자리에서 위아래로 뒤집힘
        MonthTransition.FLIP -> graphicsLayer {
            val o = offset().coerceIn(-1f, 1f)
            translationX = o * size.width
            rotationX = o * FLIP_DEGREES
            cameraDistance = FLIP_CAMERA * density
            alpha = if (abs(o) > HALF) 0f else 1f
        }
    }
}

// 애니메이션 줄이기면 넘김도 100ms
@Composable
fun monthFlingBehavior(pagerState: PagerState, reduceMotion: Boolean): TargetedFlingBehavior =
    if (reduceMotion) {
        PagerDefaults.flingBehavior(pagerState, snapAnimationSpec = tween(REDUCED_TRANSITION_MS))
    } else {
        PagerDefaults.flingBehavior(pagerState)
    }

private const val FADE = 0.6f
private const val PAPER_SCALE = 0.04f
private const val GRAIN_DOTS = 120
private const val FLIP_DEGREES = 90f
private const val FLIP_CAMERA = 12f
private const val HALF = 0.5f
