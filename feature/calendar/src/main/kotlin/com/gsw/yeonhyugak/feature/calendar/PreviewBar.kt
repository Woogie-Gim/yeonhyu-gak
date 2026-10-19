package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.ui.theme.YhTheme

const val CALENDAR_PREVIEW_BAR_TAG = "cal_preview_bar"
const val CALENDAR_PREVIEW_SUMMARY_TAG = "cal_preview_summary"
const val CALENDAR_PREVIEW_ADD_TAG = "cal_preview_add"
const val CALENDAR_PREVIEW_CANCEL_TAG = "cal_preview_cancel"

// 추천 미리보기 하단 바. 엄지가 닿는 곳에 담기 버튼
@Composable
internal fun PreviewBar(summary: PreviewSummary, onAdd: () -> Unit, onCancel: () -> Unit) {
    val colors = YhTheme.colors
    val text = if (summary.spans.size <= 1) {
        stringResource(R.string.cal_preview_one, summary.cost, summary.totalDays)
    } else {
        stringResource(
            R.string.cal_preview_many,
            summary.cost,
            summary.spans.size,
            summary.totalDays,
        )
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag(CALENDAR_PREVIEW_BAR_TAG),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(CALENDAR_PREVIEW_SUMMARY_TAG),
        )
        Text(
            stringResource(R.string.cal_preview_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.heightIn(min = 48.dp).testTag(CALENDAR_PREVIEW_CANCEL_TAG),
            ) {
                Text(stringResource(R.string.cal_preview_cancel))
            }
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag(CALENDAR_PREVIEW_ADD_TAG),
            ) {
                Text(stringResource(R.string.cal_preview_add))
            }
        }
    }
}

// 하단: 추천 미리보기 바가 있으면 그것만, 없으면 첫 진입 코치마크
@Composable
internal fun BottomSlot(state: CalendarUiState, viewModel: CalendarViewModel) {
    val preview = state.previewSummary
    if (preview != null) {
        PreviewBar(
            summary = preview,
            onAdd = { viewModel.finishPreview(add = true) },
            onCancel = { viewModel.finishPreview(add = false) },
        )
    } else {
        CoachMarkSlot(visible = state.coachMarkVisible, onDismiss = viewModel::dismissCoachMark)
    }
}
