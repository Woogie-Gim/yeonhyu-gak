package com.gsw.yeonhyugak.feature.recommend

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsw.yeonhyugak.core.engine.planner.CandidateFilter
import com.gsw.yeonhyugak.core.engine.planner.RecommendPeriod
import com.gsw.yeonhyugak.core.engine.planner.Recommendation
import com.gsw.yeonhyugak.core.engine.planner.Season
import com.gsw.yeonhyugak.core.engine.planner.SortOrder
import com.gsw.yeonhyugak.core.ui.DayCount
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import java.time.Month

const val REC_LIST_TAG = "rec_list"
const val REC_REMAINING_TAG = "rec_remaining"
const val REC_PERIOD_REST_TAG = "rec_period_rest"
const val REC_PERIOD_NEXT_TAG = "rec_period_next"
const val REC_AUTO_PLAN_TAG = "rec_auto_plan"
const val REC_FREE_TITLE_TAG = "rec_free_title"

fun recCardTag(index: Int): String = "rec_card_$index"

// 카드 제목. 테스트가 띠 일수와 비교한다
fun recCardTitleTag(index: Int): String = "rec_card_title_$index"

@Composable
fun RecommendRoute(
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecommendViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecommendScreen(
        state = state,
        actions = RecommendActions(
            onPeriod = viewModel::selectPeriod,
            onSeason = viewModel::toggleSeason,
            onMonth = viewModel::selectMonth,
            onMinLength = viewModel::toggleMinLength,
            onSort = viewModel::selectSort,
            onClearFilters = viewModel::clearFilters,
            onCard = {
                viewModel.preview(it)
                onOpenCalendar()
            },
            onAutoPlan = { if (viewModel.previewAutoPlan()) onOpenCalendar() },
        ),
        modifier = modifier,
    )
}

internal data class RecommendActions(
    val onPeriod: (RecommendPeriod) -> Unit,
    val onSeason: (Season) -> Unit,
    val onMonth: (Month?) -> Unit,
    val onMinLength: (Int) -> Unit,
    val onSort: (SortOrder) -> Unit,
    val onClearFilters: () -> Unit,
    val onCard: (Recommendation) -> Unit,
    val onAutoPlan: () -> Unit,
)

@Composable
internal fun RecommendScreen(
    state: RecommendUiState,
    actions: RecommendActions,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoaded) {
        Box(modifier.fillMaxSize())
        return
    }
    Column(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().testTag(REC_LIST_TAG),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(state, actions) }
            when {
                state.noLeaveLeft -> {
                    item { FreeHeader() }
                    itemsIndexed(state.freeBreaks) { i, item ->
                        RecommendCard(i, item, state, free = true, onClick = actions.onCard)
                    }
                }

                state.items.isEmpty() -> item { EmptyResult(actions.onClearFilters) }

                else -> itemsIndexed(state.items) { i, item ->
                    RecommendCard(i, item, state, free = false, onClick = actions.onCard)
                }
            }
        }
        // 주요 버튼은 엄지가 닿는 하단
        if (!state.noLeaveLeft && state.items.isNotEmpty()) {
            OutlinedButton(
                onClick = actions.onAutoPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .heightIn(min = 48.dp)
                    .testTag(REC_AUTO_PLAN_TAG),
            ) {
                Text(stringResource(R.string.rec_auto_plan))
            }
        }
    }
}

@Composable
private fun Header(state: RecommendUiState, actions: RecommendActions) {
    val choice = state.choice
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val remaining = DayCount.format(state.remaining)
        Text(
            text = if (choice.period == RecommendPeriod.REST_OF_YEAR) {
                stringResource(R.string.rec_remaining, remaining)
            } else {
                stringResource(R.string.rec_remaining_next, state.year, remaining)
            },
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }.testTag(REC_REMAINING_TAG),
        )
        ChipRow {
            FilterChip(
                selected = choice.period == RecommendPeriod.REST_OF_YEAR,
                onClick = { actions.onPeriod(RecommendPeriod.REST_OF_YEAR) },
                label = { Text(stringResource(R.string.rec_period_rest)) },
                modifier = Modifier.heightIn(min = 48.dp).testTag(REC_PERIOD_REST_TAG),
            )
            NextYearChip(
                selected = choice.period == RecommendPeriod.NEXT_YEAR,
                highlighted = state.nextYearHint,
                onClick = { actions.onPeriod(RecommendPeriod.NEXT_YEAR) },
            )
        }
        if (state.nextYearHint && choice.period == RecommendPeriod.REST_OF_YEAR) {
            Text(
                stringResource(R.string.rec_period_next_hint),
                style = MaterialTheme.typography.bodySmall,
                color = YhTheme.colors.muted,
            )
        }
        if (!state.noLeaveLeft) FilterRow(choice, actions)
    }
}

// 11월 이후 강조: 굵은 테두리 + `추천` 글자 배지 + 상태 설명 (색만으로 알리지 않음)
@Composable
private fun NextYearChip(selected: Boolean, highlighted: Boolean, onClick: () -> Unit) {
    val colors = YhTheme.colors
    val badge = stringResource(R.string.rec_period_next_badge)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.rec_period_next))
                if (highlighted) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier
                            .border(1.dp, colors.accent, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        },
        border = if (highlighted) {
            BorderStroke(2.dp, colors.accent)
        } else {
            FilterChipDefaults.filterChipBorder(enabled = true, selected = selected)
        },
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { if (highlighted) stateDescription = badge }
            .testTag(REC_PERIOD_NEXT_TAG),
    )
}

// SPEC 5장 카드: 연차 2일 → 5일 휴가 / 날짜 / 효율 · 연휴 이름
@Composable
private fun RecommendCard(
    index: Int,
    item: Recommendation,
    state: RecommendUiState,
    free: Boolean,
    onClick: (Recommendation) -> Unit,
) {
    val colors = YhTheme.colors
    val details = buildList {
        if (!free) {
            add(
                stringResource(R.string.rec_card_efficiency, efficiencyText(item.efficiency)),
            )
        }
        holidayLabel(state.holidays, item)?.let {
            add(stringResource(R.string.rec_card_holiday, it))
        }
    }
    Card(
        onClick = { onClick(item) },
        modifier = Modifier.fillMaxWidth().testTag(recCardTag(index)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (free) {
                    stringResource(R.string.rec_card_free_title, item.totalDays)
                } else {
                    stringResource(R.string.rec_card_title, item.cost, item.totalDays)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag(recCardTitleTag(index)),
            )
            Text(cardRange(item), style = MaterialTheme.typography.bodyMedium)
            if (details.isNotEmpty()) {
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
            if (item.bookedInside > 0) {
                Text(
                    stringResource(R.string.rec_card_booked, item.bookedInside),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
            if (item.spansYears && item.costByYear.size > 1) {
                val (a, b) = item.costByYear.toSortedMap().entries.toList()
                Text(
                    stringResource(R.string.rec_card_years, a.key, a.value, b.key, b.value),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun FreeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.rec_free_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag(REC_FREE_TITLE_TAG),
        )
        Text(
            stringResource(R.string.rec_free_body),
            style = MaterialTheme.typography.bodyMedium,
            color = YhTheme.colors.muted,
        )
    }
}

// 빈 화면은 다음 행동을 제안 (SPEC 9장)
@Composable
private fun EmptyResult(onClear: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.rec_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.rec_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = YhTheme.colors.muted,
        )
        Button(
            onClick = onClear,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.rec_empty_clear))
        }
    }
}
