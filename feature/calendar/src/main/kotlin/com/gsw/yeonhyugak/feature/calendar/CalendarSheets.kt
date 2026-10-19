package com.gsw.yeonhyugak.feature.calendar

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.engine.SupportedYears
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import java.time.LocalDate

const val DAY_SHEET_TAG = "cal_day_sheet"
const val MEMO_FIELD_TAG = "cal_memo_field"
const val YEAR_SHEET_TAG = "cal_year_sheet"

private val MinTouch = Modifier.heightIn(min = 48.dp)

internal class DaySheetActions(
    // null = 지정 해제
    val onLeave: (LocalDate, LeaveType?) -> Unit,
    // 빈 글이면 삭제
    val onSaveMemo: (LocalDate, String) -> Unit,
    val onRangeFrom: (LocalDate) -> Unit,
    val onRangeTo: (LocalDate) -> Unit,
    val onDismiss: () -> Unit,
)

// 길게 누르기 시트: 근무일이면 연차·반차, 모든 날 메모와 범위 선택
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DaySheet(
    sheet: DaySheetState,
    isWorkday: Boolean,
    leave: LeaveType?,
    holidays: List<Holiday>,
    hasAnchor: Boolean,
    actions: DaySheetActions,
) {
    val date = sheet.date
    // 반만 펼치면 아래 버튼이 화면 밖으로 밀린다. 처음부터 전부 펼침
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(DAY_SHEET_TAG),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(dateLabel(date), style = MaterialTheme.typography.titleLarge)
            if (holidays.isNotEmpty()) {
                Text(
                    holidays.map { holidayName(it) }.joinToString(),
                    color = YhTheme.colors.holiday,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            // 비근무일에는 연차 버튼 없음. 이미 지정된 날은 해제만 가능
            if (isWorkday || leave != null) {
                LeaveSection(leave, isWorkday) { actions.onLeave(date, it) }
            }
            MemoSection(sheet) { actions.onSaveMemo(date, it) }
            RangeSection(
                hasAnchor = hasAnchor,
                onFrom = { actions.onRangeFrom(date) },
                onTo = { actions.onRangeTo(date) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeaveSection(leave: LeaveType?, isWorkday: Boolean, onLeave: (LeaveType?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LeaveButton(R.string.cal_sheet_full, leave == LeaveType.FULL, isWorkday) {
            onLeave(LeaveType.FULL)
        }
        LeaveButton(R.string.cal_sheet_am, leave == LeaveType.AM_HALF, isWorkday) {
            onLeave(LeaveType.AM_HALF)
        }
        LeaveButton(R.string.cal_sheet_pm, leave == LeaveType.PM_HALF, isWorkday) {
            onLeave(LeaveType.PM_HALF)
        }
        if (leave != null) {
            TextButton(onClick = { onLeave(null) }, modifier = MinTouch) {
                Text(stringResource(R.string.cal_sheet_clear))
            }
        }
    }
}

@Composable
private fun LeaveButton(
    @StringRes label: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(onClick = onClick, enabled = enabled, modifier = MinTouch) {
            Text(stringResource(label))
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = MinTouch) {
            Text(stringResource(label))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoSection(sheet: DaySheetState, onSave: (String) -> Unit) {
    var memo by rememberSaveable(sheet.date) { mutableStateOf(sheet.memo.orEmpty()) }
    OutlinedTextField(
        value = memo,
        onValueChange = { memo = it },
        modifier = Modifier.fillMaxWidth().testTag(MEMO_FIELD_TAG),
        label = { Text(stringResource(R.string.cal_sheet_memo_label)) },
        placeholder = { Text(stringResource(R.string.cal_sheet_memo_hint)) },
        supportingText = { Text(stringResource(R.string.cal_sheet_memo_private)) },
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onSave(memo) },
            enabled = memo.trim() != sheet.memo.orEmpty(),
            modifier = MinTouch,
        ) {
            Text(stringResource(R.string.cal_sheet_memo_save))
        }
        if (sheet.memo != null) {
            TextButton(onClick = { onSave("") }, modifier = MinTouch) {
                Text(stringResource(R.string.cal_sheet_memo_delete))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeSection(hasAnchor: Boolean, onFrom: () -> Unit, onTo: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onFrom, modifier = MinTouch) {
            Text(stringResource(R.string.cal_range_from))
        }
        OutlinedButton(onClick = onTo, enabled = hasAnchor, modifier = MinTouch) {
            Text(stringResource(R.string.cal_range_to))
        }
    }
}

// 연도 선택: 예상 연도는 글자로 표시
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun YearPickerSheet(
    current: Int,
    isExpected: (Int) -> Boolean,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.testTag(YEAR_SHEET_TAG)) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(R.string.cal_year_picker_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SupportedYears.range.forEach { year ->
                    val label = if (isExpected(year)) {
                        stringResource(R.string.cal_year_item_expected, year)
                    } else {
                        stringResource(R.string.cal_year_item, year)
                    }
                    FilterChip(
                        selected = year == current,
                        onClick = { onPick(year) },
                        label = { Text(label) },
                        modifier = MinTouch,
                    )
                }
            }
        }
    }
}
