package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.engine.calendar.MonthGrid
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import java.time.LocalDate

const val CALENDAR_GRID_TAG = "cal_grid"
const val CALENDAR_WEEKDAY_TAG = "cal_weekday"

fun cellTag(date: LocalDate): String = "cal_cell_$date"

internal data class GridCallbacks(
    val onTap: (LocalDate) -> Unit,
    val onLongPress: (LocalDate) -> Unit,
    val onDragStart: (LocalDate) -> Unit,
    val onDragMove: (LocalDate) -> Unit,
    val onDragEnd: (LocalDate, LocalDate) -> Unit,
    val onDragCancel: () -> Unit,
    val onRangeFrom: (LocalDate) -> Unit,
    val onRangeTo: (LocalDate) -> Unit,
)

@Composable
internal fun WeekdayHeader(modifier: Modifier = Modifier) {
    val colors = YhTheme.colors
    val names = stringArrayResource(R.array.cal_weekday_short)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .testTag(CALENDAR_WEEKDAY_TAG)
            .clearAndSetSemantics { },
    ) {
        names.forEachIndexed { i, name ->
            Text(
                text = name,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = when (i) {
                    0 -> colors.holiday
                    SATURDAY_INDEX -> colors.saturday
                    else -> colors.muted
                },
            )
        }
    }
}

@Composable
internal fun MonthGridView(
    grid: MonthGrid,
    today: LocalDate,
    memoDates: Set<LocalDate>,
    selection: ClosedRange<LocalDate>?,
    rowHeight: Dp,
    callbacks: GridCallbacks,
    modifier: Modifier = Modifier,
    newHolidayDates: Set<LocalDate> = emptySet(),
) {
    val currentGrid by rememberUpdatedState(grid)
    val currentCallbacks by rememberUpdatedState(callbacks)
    val labels = CellActionLabels(
        mark = stringResource(R.string.cal_a11y_action_mark),
        clear = stringResource(R.string.cal_a11y_action_clear),
        name = stringResource(R.string.cal_a11y_action_name),
        options = stringResource(R.string.cal_a11y_action_options),
        from = stringResource(R.string.cal_range_from),
        to = stringResource(R.string.cal_range_to),
    )
    Column(
        modifier
            .fillMaxWidth()
            .height(rowHeight * MonthGrid.WEEKS)
            .testTag(CALENDAR_GRID_TAG)
            .calendarGestures({ currentGrid }, { currentCallbacks }),
    ) {
        grid.weeks.forEach { week ->
            Row(Modifier.fillMaxWidth().height(rowHeight)) {
                week.forEach { cell ->
                    val cellModifier = Modifier.weight(1f).height(rowHeight)
                    if (cell.inMonth) {
                        val date = cell.date
                        val selected = selection?.contains(date) == true
                        val isNew = date in newHolidayDates && cell.isHoliday
                        val description =
                            cellDescription(cell, date == today, date in memoDates, selected, isNew)
                        DayCellView(
                            cell = cell,
                            isToday = date == today,
                            hasMemo = date in memoDates,
                            selected = selected,
                            isNew = isNew,
                            modifier = cellModifier.cellSemantics(
                                cell,
                                description,
                                labels,
                                callbacks,
                            ),
                        )
                    } else {
                        Box(cellModifier)
                    }
                }
            }
        }
    }
}

private const val SATURDAY_INDEX = 6
