package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.gsw.yeonhyugak.core.ui.rememberNotificationPermission
import com.gsw.yeonhyugak.core.ui.theme.YhTheme

// 첫 담기 직후 한 번. 알려주세요 → 시스템 권한, 괜찮아요 → 끔
@Composable
internal fun NotifyAskDialog(onAnswer: (on: Boolean) -> Unit) {
    val requester = rememberNotificationPermission()
    val colors = YhTheme.colors
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        containerColor = colors.background,
        titleContentColor = colors.ink,
        textContentColor = colors.ink,
        title = { Text(stringResource(R.string.cal_notify_title)) },
        text = { Text(stringResource(R.string.cal_notify_body)) },
        confirmButton = {
            TextButton(onClick = { requester.request(onAnswer) }) {
                Text(stringResource(R.string.cal_notify_yes), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = { onAnswer(false) }) {
                Text(stringResource(R.string.cal_notify_no), color = colors.ink)
            }
        },
        modifier = Modifier.testTag(NOTIFY_ASK_TAG),
    )
}
