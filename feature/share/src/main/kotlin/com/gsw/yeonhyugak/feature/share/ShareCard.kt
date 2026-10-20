package com.gsw.yeonhyugak.feature.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gsw.yeonhyugak.core.data.holiday.HolidayLabels
import com.gsw.yeonhyugak.core.ui.DayCount
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.core.ui.theme.numeralText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

const val SHARE_CARD_TAG = "share_card"
const val SHARE_QR_TAG = "share_qr"

// 카드 폭은 고정. 비율에 따라 높이만 바뀐다
val SHARE_CARD_WIDTH: Dp = 320.dp

fun ShareRatio.heightFor(width: Dp): Dp = width * height / this.width

// 현재 테마 그대로. 글자 크기 설정과 무관하게 같은 그림이 나오도록 sp 고정 크기
@Composable
fun ShareCard(
    data: ShareCardData,
    ratio: ShareRatio,
    appName: String,
    modifier: Modifier = Modifier,
) {
    val colors = YhTheme.colors
    val fonts = YhTheme.fonts
    val shown = data.blocks.take(ratio.maxRows)
    Column(
        modifier
            .width(SHARE_CARD_WIDTH)
            .height(ratio.heightFor(SHARE_CARD_WIDTH))
            .testTag(SHARE_CARD_TAG)
            .background(colors.background)
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                fonts.numeralText(stringResource(R.string.share_card_title, data.year)),
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                color = colors.ink,
            )
            if (data.expected) {
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.share_card_expected),
                    style = TextStyle(fontSize = 12.sp),
                    color = colors.holiday,
                    modifier = Modifier
                        .border(1.dp, colors.holiday, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(
                R.string.share_card_summary,
                data.totalDays,
                DayCount.format(data.usedLeave),
            ),
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = colors.accent,
        )
        Spacer(Modifier.height(18.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shown.forEach { BlockRow(it) }
            val hidden = data.blocks.size - shown.size
            if (hidden > 0) {
                Text(
                    stringResource(R.string.share_card_more, hidden),
                    style = TextStyle(fontSize = 13.sp),
                    color = colors.muted,
                )
            }
        }
        Footer(data.showQr, appName)
    }
}

@Composable
private fun BlockRow(block: ShareBlock) {
    val colors = YhTheme.colors
    val res = LocalResources.current
    val format = DateTimeFormatter.ofPattern(
        stringResource(R.string.share_card_date),
        Locale.KOREAN,
    )
    val days = stringResource(R.string.share_card_days, block.days)
    val name = block.holiday?.let { h ->
        HolidayLabels.displayName(h) { id, args -> res.getString(id, *args) }
    }
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        // 연휴 띠와 같은 모양의 막대
        Box(
            Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(colors.leaveFill, RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    R.string.share_card_range,
                    block.start.label(format),
                    block.end.label(format),
                ),
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(days, name).joinToString(" · "),
                style = TextStyle(fontSize = 12.sp),
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Footer(showQr: Boolean, appName: String) {
    val colors = YhTheme.colors
    val state = YhTheme.state
    // QR은 다크 모드에서도 밝은 바탕 + 어두운 칸
    val light = state.spec.colors(dark = false, month = state.month, cheer = state.appearance.cheer)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            appName,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = colors.muted,
            modifier = Modifier.weight(1f),
        )
        if (showQr) {
            QrCode(
                text = PLAY_STORE_URL,
                dark = light.ink,
                light = light.background,
                description = stringResource(R.string.share_card_qr),
                modifier = Modifier.size(64.dp).testTag(SHARE_QR_TAG),
            )
        }
    }
}

private fun LocalDate.label(format: DateTimeFormatter): String = format(format)
