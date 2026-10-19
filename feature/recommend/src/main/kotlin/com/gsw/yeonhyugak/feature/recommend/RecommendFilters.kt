package com.gsw.yeonhyugak.feature.recommend

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.engine.planner.CandidateFilter
import com.gsw.yeonhyugak.core.engine.planner.Season
import com.gsw.yeonhyugak.core.engine.planner.SortOrder
import java.time.Month

// 필터 칩: 최소 길이 · 계절 · 월 · 정렬. 가로로 밀어서 본다
@Composable
internal fun FilterRow(choice: RecommendChoice, actions: RecommendActions) {
    val seasons = stringArrayResource(R.array.rec_seasons)
    ChipRow {
        CandidateFilter.MIN_LENGTH_CHIPS.forEach { days ->
            FilterChip(
                selected = choice.minLength == days,
                onClick = { actions.onMinLength(days) },
                label = { Text(stringResource(R.string.rec_min_length, days)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
        Season.entries.forEachIndexed { i, season ->
            FilterChip(
                selected = choice.season == season,
                onClick = { actions.onSeason(season) },
                label = { Text(seasons[i]) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
        MonthMenu(choice.month, actions.onMonth)
        SortMenu(choice.sort, actions.onSort)
    }
}

@Composable
private fun MonthMenu(month: Month?, onMonth: (Month?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = month != null,
            onClick = { open = true },
            label = {
                Text(
                    month?.let { stringResource(R.string.rec_month_item, it.value) }
                        ?: stringResource(R.string.rec_month_any),
                )
            },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.rec_month_any)) },
                onClick = {
                    open = false
                    onMonth(null)
                },
            )
            Month.entries.forEach { m ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.rec_month_item, m.value)) },
                    onClick = {
                        open = false
                        onMonth(m)
                    },
                )
            }
        }
    }
}

@Composable
private fun SortMenu(sort: SortOrder, onSort: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val names = mapOf(
        SortOrder.EFFICIENCY to stringResource(R.string.rec_sort_efficiency),
        SortOrder.LENGTH to stringResource(R.string.rec_sort_length),
        SortOrder.DATE to stringResource(R.string.rec_sort_date),
    )
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.rec_sort_label, names.getValue(sort)))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOrder.entries.forEach { s ->
                DropdownMenuItem(
                    text = { Text(names.getValue(s)) },
                    onClick = {
                        open = false
                        onSort(s)
                    },
                )
            }
        }
    }
}

@Composable
internal fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}
