package com.gsw.yeonhyugak.feature.recommend.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsw.yeonhyugak.core.engine.leave.LeaveBasis
import com.gsw.yeonhyugak.core.ui.DayCount
import com.gsw.yeonhyugak.core.ui.DayStepper
import com.gsw.yeonhyugak.core.ui.StepperLabels
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.feature.recommend.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

const val ONBOARDING_TAG = "onb_root"
const val ONBOARDING_START_TAG = "onb_start"
const val ONBOARDING_UNKNOWN_TAG = "onb_unknown"
const val ONBOARDING_HIRE_TAG = "onb_hire"

@Composable
fun OnboardingRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        onRemaining = viewModel::setRemaining,
        onUnknown = viewModel::openHire,
        hire = HireActions(
            onDate = viewModel::setHireDate,
            onBasis = viewModel::setBasis,
            onApply = viewModel::applyHire,
            onClose = viewModel::closeHire,
        ),
        onStart = { viewModel.complete(onDone) },
        modifier = modifier,
    )
}

internal data class HireActions(
    val onDate: (LocalDate) -> Unit,
    val onBasis: (LeaveBasis) -> Unit,
    val onApply: () -> Unit,
    val onClose: () -> Unit,
)

@Composable
internal fun OnboardingScreen(
    state: OnboardingUiState,
    onRemaining: (Double) -> Unit,
    onUnknown: () -> Unit,
    hire: HireActions,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val value = DayCount.format(state.remaining)
    Column(modifier.fillMaxSize().testTag(ONBOARDING_TAG)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.heightIn(min = 24.dp))
            Text(
                stringResource(R.string.onb_question),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            DayStepper(
                value = state.remaining,
                onValueChange = onRemaining,
                labels = StepperLabels(
                    value = stringResource(R.string.onb_value, value),
                    minus = stringResource(R.string.onb_minus),
                    plus = stringResource(R.string.onb_plus),
                ),
            )
            Text(
                stringResource(R.string.onb_later_note),
                style = MaterialTheme.typography.bodySmall,
                color = YhTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
            if (state.hireOpen) {
                HirePanel(state, hire)
            } else {
                TextButton(
                    onClick = onUnknown,
                    modifier = Modifier.heightIn(min = 48.dp).testTag(ONBOARDING_UNKNOWN_TAG),
                ) {
                    Text(stringResource(R.string.onb_unknown))
                }
            }
        }
        // 엄지가 닿는 하단
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .heightIn(min = 56.dp)
                .testTag(ONBOARDING_START_TAG),
        ) {
            Text(stringResource(R.string.onb_start), style = MaterialTheme.typography.titleMedium)
        }
    }
}

// 보조 경로: 입사일 → 엔진 계산값으로 스테퍼 채우기
@Composable
private fun HirePanel(state: OnboardingUiState, hire: HireActions) {
    val colors = YhTheme.colors
    var picking by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().testTag(ONBOARDING_HIRE_TAG),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.onb_hire_title),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(onClick = {
                picking = true
            }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(
                    state.hireDate?.let {
                        stringResource(
                            R.string.onb_hire_date,
                            it.year,
                            it.monthValue,
                            it.dayOfMonth,
                        )
                    } ?: stringResource(R.string.onb_hire_pick),
                )
            }
            BasisChips(state.basis, hire.onBasis)
            state.calculated?.let {
                Text(
                    stringResource(R.string.onb_hire_result, DayCount.format(it)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(
                stringResource(R.string.onb_reference),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            HireButtons(applyEnabled = state.calculated != null, hire = hire)
        }
    }
    if (picking) {
        HireDatePicker(
            today = state.today,
            initial = state.hireDate,
            onPick = {
                picking = false
                hire.onDate(it)
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun BasisChips(basis: LeaveBasis, onBasis: (LeaveBasis) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = basis == LeaveBasis.ANNIVERSARY,
            onClick = { onBasis(LeaveBasis.ANNIVERSARY) },
            label = { Text(stringResource(R.string.onb_basis_anniversary)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        FilterChip(
            selected = basis == LeaveBasis.FISCAL_YEAR,
            onClick = { onBasis(LeaveBasis.FISCAL_YEAR) },
            label = { Text(stringResource(R.string.onb_basis_fiscal)) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
    }
}

@Composable
private fun HireButtons(applyEnabled: Boolean, hire: HireActions) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = hire.onClose, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.onb_hire_cancel))
        }
        Button(
            onClick = hire.onApply,
            enabled = applyEnabled,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.onb_hire_apply))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HireDatePicker(
    today: LocalDate,
    initial: LocalDate?,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayMillis = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay()?.toInstant(
            ZoneOffset.UTC,
        )?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis <= todayMillis
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    picker.selectedDateMillis?.let {
                        onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    } ?: onDismiss()
                },
            ) { Text(stringResource(R.string.onb_hire_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.onb_hire_cancel)) }
        },
    ) {
        DatePicker(state = picker)
    }
}
