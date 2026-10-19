package com.gsw.yeonhyugak.feature.leave

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.DayCount
import com.gsw.yeonhyugak.core.ui.DayStepper
import com.gsw.yeonhyugak.core.ui.StepperLabels
import com.gsw.yeonhyugak.core.ui.showUndo
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val LEAVE_REMAINING_TAG = "leave_remaining"
const val LEAVE_EDIT_TAG = "leave_edit"
const val LEAVE_EDIT_SAVE_TAG = "leave_edit_save"
const val LEAVE_EMPTY_TAG = "leave_empty"

fun leaveRecordTag(index: Int): String = "leave_record_$index"

@Composable
fun LeaveRoute(
    snackbarHostState: SnackbarHostState,
    onOpenRecommend: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LeaveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message = stringResource(R.string.leave_done_edit)
    val undoLabel = stringResource(R.string.leave_undo)
    val a11y = LocalAccessibilityManager.current
    LaunchedEffect(viewModel) {
        var job: Job? = null
        viewModel.eventFlow.collect { event ->
            job?.cancel()
            job = launch {
                if (snackbarHostState.showUndo(message, undoLabel, a11y)) viewModel.undo(event)
            }
        }
    }
    LeaveScreen(
        state = state,
        onEdit = viewModel::startEdit,
        onOpenRecommend = onOpenRecommend,
        modifier = modifier,
    )
    if (state.editing) {
        EditRemainingSheet(
            initial = state.remaining.coerceAtLeast(0.0),
            onSave = viewModel::saveRemaining,
            onDismiss = viewModel::cancelEdit,
        )
    }
}

@Composable
internal fun LeaveScreen(
    state: LeaveUiState,
    onEdit: () -> Unit,
    onOpenRecommend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.loaded) {
        Box(modifier.fillMaxSize())
        return
    }
    val colors = YhTheme.colors
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RemainingSummary(state, onEdit)
        state.expiresOn?.let {
            Text(
                stringResource(R.string.leave_expiry, it.year, it.monthValue, it.dayOfMonth),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            stringResource(R.string.leave_reference),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted,
        )
        HorizontalDivider(color = colors.outline)
        Text(
            stringResource(R.string.leave_history),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (state.records.isEmpty()) {
            EmptyHistory(onOpenRecommend)
        } else {
            state.records.forEachIndexed { i, record -> RecordRow(i, record) }
        }
    }
}

// 남은 개수 크게 + 직접 수정
@Composable
private fun RemainingSummary(state: LeaveUiState, onEdit: () -> Unit) {
    val colors = YhTheme.colors
    Text(
        stringResource(R.string.leave_remaining_label),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        stringResource(R.string.leave_days, DayCount.format(state.remaining)),
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Bold,
        color = if (state.remaining < 0) colors.warning else colors.ink,
        modifier = Modifier.testTag(LEAVE_REMAINING_TAG),
    )
    // 초과는 색 + 글자로
    if (state.remaining < 0) {
        Text(
            stringResource(R.string.leave_over, DayCount.format(-state.remaining)),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.warning,
        )
    }
    Text(
        stringResource(
            R.string.leave_summary,
            DayCount.format(state.allowance),
            DayCount.format(state.used),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.muted,
    )
    OutlinedButton(
        onClick = onEdit,
        modifier = Modifier.heightIn(min = 48.dp).testTag(LEAVE_EDIT_TAG),
    ) {
        Text(stringResource(R.string.leave_edit))
    }
}

@Composable
private fun RecordRow(index: Int, record: LeaveRecord) {
    val weekday = stringArrayResource(R.array.leave_weekday_short)[record.date.dayOfWeek.value % 7]
    val type = stringResource(
        when (record.type) {
            LeaveType.FULL -> R.string.leave_type_full
            LeaveType.AM_HALF -> R.string.leave_type_am
            LeaveType.PM_HALF -> R.string.leave_type_pm
        },
    )
    Text(
        stringResource(
            R.string.leave_record,
            record.date.monthValue,
            record.date.dayOfMonth,
            weekday,
            type,
        ),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp).testTag(leaveRecordTag(index)),
    )
}

// 빈 화면은 다음 행동을 제안 (SPEC 9장)
@Composable
private fun EmptyHistory(onOpenRecommend: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().testTag(LEAVE_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.leave_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onOpenRecommend, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.leave_go_recommend))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRemainingSheet(initial: Double, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableDoubleStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = YhTheme.colors.background) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.leave_edit_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            DayStepper(
                value = value,
                onValueChange = { value = it },
                labels = StepperLabels(
                    value = stringResource(R.string.leave_days, DayCount.format(value)),
                    minus = stringResource(R.string.leave_edit_minus),
                    plus = stringResource(R.string.leave_edit_plus),
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.leave_edit_cancel))
                }
                Button(
                    onClick = { onSave(value) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag(LEAVE_EDIT_SAVE_TAG),
                ) {
                    Text(stringResource(R.string.leave_edit_save))
                }
            }
        }
    }
}
