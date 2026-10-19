package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.ui.theme.YhTheme

// 코치마크 자리는 이번 화면 동안 유지. 닫혀도 달력이 움직이지 않는다
@Composable
internal fun CoachMarkSlot(visible: Boolean, onDismiss: () -> Unit) {
    var slot by rememberSaveable { mutableStateOf(false) }
    if (visible) slot = true
    if (slot) CoachMark(visible, onDismiss)
}

// 첫 진입 1회. 하단에 두어 달력을 가리지 않는다. 닫히면 내용만 숨김
@Composable
private fun CoachMark(visible: Boolean, onDismiss: () -> Unit) {
    val colors = YhTheme.colors
    val shown = if (visible) {
        Modifier.testTag(CALENDAR_COACH_TAG)
    } else {
        Modifier.alpha(0f).clearAndSetSemantics { }
    }
    Row(
        shown
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            stringResource(R.string.cal_coach),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        TextButton(
            onClick = onDismiss,
            enabled = visible,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.cal_coach_ok))
        }
    }
}
