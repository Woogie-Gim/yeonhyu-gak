package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.engine.calendar.BlockBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 화살촉은 다음 줄로 넘어가는 끝에만. 이어받는 시작은 평평
class BandEdgesTest {
    private fun band(
        isStart: Boolean = false,
        isEnd: Boolean = false,
        continuesPrev: Boolean = false,
        continuesNext: Boolean = false,
    ) = BlockBand(
        length = 5,
        isStart = isStart,
        isEnd = isEnd,
        labelHere = false,
        continuesPrev = continuesPrev,
        continuesNext = continuesNext,
    )

    @Test
    fun continuedStart_isFlat_noArrow() {
        // 5/3(일): 윗줄에서 이어받고 블록 중간
        val edges = bandEdges(band(continuesPrev = true))
        assertEquals(0.dp, edges.startInset)
        assertFalse(edges.arrowAtEnd)
    }

    @Test
    fun continuedStart_andBlockEnd_flatStartRoundEnd() {
        val edges = bandEdges(band(isEnd = true, continuesPrev = true))
        assertEquals(0.dp, edges.startInset)
        assertEquals(4.dp, edges.endInset)
        assertFalse(edges.arrowAtEnd)
    }

    @Test
    fun rowEnd_hasArrow() {
        // 5/2(토): 다음 줄로 넘어감
        val edges = bandEdges(band(continuesNext = true))
        assertTrue(edges.arrowAtEnd)
        assertEquals(0.dp, edges.startInset)
    }

    @Test
    fun blockStartAndEnd_keepRoundInset() {
        val edges = bandEdges(band(isStart = true, isEnd = true))
        assertEquals(4.dp, edges.startInset)
        assertEquals(4.dp, edges.endInset)
        assertFalse(edges.arrowAtEnd)
    }
}
