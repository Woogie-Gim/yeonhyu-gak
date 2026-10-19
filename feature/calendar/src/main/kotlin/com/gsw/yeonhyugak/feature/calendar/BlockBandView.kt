package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gsw.yeonhyugak.core.engine.calendar.BlockBand
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.core.ui.theme.numeralText
import java.time.LocalDate

// 칸 단위 테스트용. 실제 화면은 cellSemantics가 덮어쓴다
internal fun bandTag(date: LocalDate): String = "cal_band_$date"

// 띠 끝 모양. 블록 시작·끝은 띄워서 둥글게, 다음 줄로 넘는 끝만 화살촉. 이어받는 시작은 평평
internal data class BandEdges(val startInset: Dp, val endInset: Dp, val arrowAtEnd: Boolean)

internal fun bandEdges(band: BlockBand): BandEdges = BandEdges(
    startInset = if (band.isStart) BAND_INSET else 0.dp,
    // 화살촉 자리만큼 띠를 줄인다
    endInset = when {
        band.isEnd -> BAND_INSET
        band.continuesNext -> ARROW_WIDTH
        else -> 0.dp
    },
    arrowAtEnd = band.continuesNext,
)

// 연휴 띠: 블록 전체를 잇는 띠. 줄을 넘으면 끝에 화살촉
@Composable
internal fun BandRow(band: BlockBand, date: LocalDate) {
    val color = YhTheme.colors.bandFill
    Box(Modifier.fillMaxSize()) {
        BandLine(band, color, date, Modifier.align(Alignment.CenterStart))
        if (bandEdges(band).arrowAtEnd) ContinueArrow(color)
    }
}

// 블록 시작 칸 띠 아래 `n일`
@Composable
internal fun BandLabel(band: BlockBand) {
    AutoText(
        text = YhTheme.fonts.numeralText(stringResource(R.string.cal_band_days, band.length)),
        color = YhTheme.colors.bandLabel,
        maxSize = 12.sp,
        align = TextAlign.Start,
        modifier = Modifier.fillMaxSize().padding(start = BAND_INSET),
    )
}

@Composable
private fun BandLine(band: BlockBand, color: Color, date: LocalDate, modifier: Modifier) {
    val shape = RoundedCornerShape(
        topStart = if (band.isStart) 2.dp else 0.dp,
        bottomStart = if (band.isStart) 2.dp else 0.dp,
        topEnd = if (band.isEnd) 2.dp else 0.dp,
        bottomEnd = if (band.isEnd) 2.dp else 0.dp,
    )
    val edges = bandEdges(band)
    Box(
        modifier
            .padding(start = edges.startInset, end = edges.endInset)
            .fillMaxWidth()
            .height(BAND_THICKNESS)
            .clip(shape)
            .background(color)
            .testTag(bandTag(date)),
    )
}

// 이어짐 표시: 줄 끝 ▶ = 다음 줄로 넘어감
@Composable
private fun ContinueArrow(color: Color) {
    Box(
        Modifier.fillMaxSize().drawBehind {
            val w = ARROW_WIDTH.toPx()
            drawPath(arrowPath(size.width - w, w, size.height), color)
        },
    )
}

private fun arrowPath(left: Float, width: Float, height: Float) = Path().apply {
    moveTo(left, 0f)
    lineTo(left + width, height / 2)
    lineTo(left, height)
    close()
}

private val BAND_THICKNESS = 4.dp
private val BAND_INSET = 4.dp
private val ARROW_WIDTH = 6.dp
