package com.gsw.yeonhyugak.dev

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.R
import com.gsw.yeonhyugak.core.ui.R as CoreUiR
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import kotlinx.coroutines.launch

const val DEV_MENU_TAG = "dev_menu"
const val DEV_ALERTS_TAG = "dev_alerts"
const val DEV_SYNC_TAG = "dev_sync"
const val DEV_WIDGETS_TAG = "dev_widgets"
const val DEV_RESULT_TAG = "dev_result"

// 테스트는 가짜 작업을 넣는다
class DevActions(
    val sendAlerts: suspend () -> String,
    val syncHolidays: suspend () -> String,
    val refreshWidgets: suspend () -> String,
)

@Composable
fun DevMenuScreen(actions: DevActions, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    val running = stringResource(R.string.dev_running)
    BackHandler(onBack = onBack)
    fun run(job: suspend () -> String) {
        result = running
        scope.launch { result = job() }
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(DEV_MENU_TAG),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(CoreUiR.drawable.ic_back),
                    contentDescription = null,
                    tint = YhTheme.colors.ink,
                )
            }
            Text(
                stringResource(R.string.dev_menu_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        }
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.dev_menu_note),
                style = MaterialTheme.typography.bodySmall,
                color = YhTheme.colors.muted,
            )
            DevButton(R.string.dev_send_alerts, R.string.dev_send_alerts_desc, DEV_ALERTS_TAG) {
                run(actions.sendAlerts)
            }
            DevButton(R.string.dev_sync_holidays, R.string.dev_sync_holidays_desc, DEV_SYNC_TAG) {
                run(actions.syncHolidays)
            }
            DevButton(
                R.string.dev_refresh_widgets,
                R.string.dev_refresh_widgets_desc,
                DEV_WIDGETS_TAG,
            ) {
                run(actions.refreshWidgets)
            }
            result?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag(DEV_RESULT_TAG),
                )
            }
        }
    }
}

@Composable
private fun DevButton(
    @StringRes label: Int,
    @StringRes desc: Int,
    tag: String,
    onClick: () -> Unit,
) {
    val colors = YhTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag),
        ) {
            Text(stringResource(label))
        }
        Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = colors.muted)
    }
}
