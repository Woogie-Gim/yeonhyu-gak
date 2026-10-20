package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.engine.calendar.RangeSummary
import com.gsw.yeonhyugak.core.ui.DayCount
import com.gsw.yeonhyugak.core.ui.R as CoreUiR
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.core.ui.theme.numeralText
import java.time.LocalDate
import java.time.YearMonth

// 상단 바: 연도·월 제목 · 예상 배지 · 오늘 · 공유 · 설정. 말풍선
@Composable
internal fun TopBar(
    month: YearMonth,
    expected: Boolean,
    onTitle: () -> Unit,
    onExpected: () -> Unit,
    onToday: () -> Unit,
    onSettings: () -> Unit,
    onShare: () -> Unit = {},
) {
    val colors = YhTheme.colors
    val fonts = YhTheme.fonts
    val title = stringResource(R.string.cal_title_month, month.year, month.monthValue)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.cal_title_hint),
                    onClick = onTitle,
                )
                .padding(horizontal = 8.dp)
                .semantics { heading() }
                .testTag(CALENDAR_TITLE_TAG),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                fonts.numeralText(
                    stringResource(R.string.cal_title_month_picker, month.year, month.monthValue),
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = title
                },
            )
        }
        if (expected) ExpectedBadge(onExpected)
        Box(Modifier.weight(1f))
        TextButton(
            onClick = onToday,
            modifier = Modifier.heightIn(min = 48.dp).testTag(CALENDAR_TODAY_TAG),
        ) {
            Text(stringResource(R.string.cal_today), color = colors.todayText)
        }
        IconButton(onClick = onShare, modifier = Modifier.testTag(CALENDAR_SHARE_TAG)) {
            Icon(
                painterResource(CoreUiR.drawable.ic_share),
                contentDescription = stringResource(R.string.cal_share),
                tint = colors.ink,
            )
        }
        IconButton(onClick = onSettings, modifier = Modifier.testTag(CALENDAR_SETTINGS_TAG)) {
            Icon(
                painterResource(CoreUiR.drawable.ic_settings),
                contentDescription = stringResource(R.string.cal_settings),
                tint = colors.ink,
            )
        }
    }
}

@Composable
internal fun bubbleText(summary: RangeSummary?, anchor: LocalDate?, tip: Tip?): String? = when {
    summary != null && summary.isOver -> stringResource(
        R.string.cal_bubble_range_over,
        DayCount.format(summary.cost),
        summary.totalDays,
        DayCount.format(summary.overBy),
    )

    summary != null ->
        stringResource(R.string.cal_bubble_range, DayCount.format(summary.cost), summary.totalDays)

    anchor != null -> stringResource(R.string.cal_bubble_anchor, dateLabel(anchor))

    tip is Tip.Holidays -> tip.holidays.map { holidayName(it) }.joinToString()

    tip == Tip.Weekend -> stringResource(R.string.cal_bubble_weekend)

    tip == Tip.Expected -> stringResource(R.string.cal_expected_note)

    else -> null
}

// 드래그 요약·공휴일 이름 말풍선. 초과면 경고색 + 글자
@Composable
internal fun Bubble(text: String?, warning: Boolean) {
    val colors = YhTheme.colors
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (text != null) {
            Text(
                text = text,
                color = if (warning) colors.onWarning else colors.background,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .background(
                        if (warning) colors.warning else colors.ink,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(CALENDAR_BUBBLE_TAG),
            )
        }
    }
}

// 예상 배지: 글자 + 테두리. 누르면 설명
@Composable
private fun ExpectedBadge(onClick: () -> Unit) {
    val colors = YhTheme.colors
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.cal_badge_expected),
            color = colors.holiday,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .border(1.dp, colors.holiday, CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
