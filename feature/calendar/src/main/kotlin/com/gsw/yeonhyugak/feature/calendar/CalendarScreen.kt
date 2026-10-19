package com.gsw.yeonhyugak.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsw.yeonhyugak.core.engine.SupportedYears
import com.gsw.yeonhyugak.core.engine.calendar.MonthGrid
import com.gsw.yeonhyugak.core.engine.calendar.RangeSummary
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.ui.rememberReduceMotion
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

const val CALENDAR_TITLE_TAG = "cal_title"
const val CALENDAR_TODAY_TAG = "cal_today"
const val CALENDAR_BUBBLE_TAG = "cal_bubble"
const val CALENDAR_COACH_TAG = "cal_coach"
const val CALENDAR_PAGER_TAG = "cal_pager"

private const val UNDO_MS = 5_000L
private const val TIP_MS = 2_500L
private val MIN_ROW = 52.dp

private val FIRST_MONTH = YearMonth.of(SupportedYears.FIRST, 1)
private const val MONTHS_PER_YEAR = 12
private val PAGE_COUNT = SupportedYears.range.count() * MONTHS_PER_YEAR

internal fun pageOf(month: YearMonth): Int =
    ((month.year - FIRST_MONTH.year) * MONTHS_PER_YEAR + month.monthValue - 1).coerceIn(
        0,
        PAGE_COUNT - 1,
    )

internal fun monthOf(page: Int): YearMonth = FIRST_MONTH.plusMonths(page.toLong())

// 말풍선 안내 (드래그 요약이 없을 때)
private sealed interface Tip {
    data class Holidays(val holidays: List<Holiday>) : Tip

    data object Weekend : Tip

    data object Expected : Tip
}

@Composable
fun CalendarRoute(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val res = LocalResources.current
    val a11y = LocalAccessibilityManager.current
    var tip by remember { mutableStateOf<Tip?>(null) }
    val undoLabel = stringResource(R.string.cal_undo)

    LaunchedEffect(viewModel) {
        var undoJob: Job? = null
        viewModel.eventFlow.collect { event ->
            when (event) {
                is CalendarEvent.Changed -> {
                    undoJob?.cancel()
                    undoJob = launch {
                        val message = res.getString(
                            event.message.res,
                            *event.message.args.toTypedArray(),
                        )
                        // TalkBack 사용자는 권장 시간만큼 더 길게
                        val timeout = a11y?.calculateRecommendedTimeoutMillis(
                            UNDO_MS,
                            containsIcons = false,
                            containsText = true,
                            containsControls = event.undoable,
                        ) ?: UNDO_MS
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = withTimeoutOrNull(timeout) {
                            snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = if (event.undoable) undoLabel else null,
                                duration = SnackbarDuration.Indefinite,
                            )
                        }
                        if (result == SnackbarResult.ActionPerformed) viewModel.undo()
                    }
                }

                is CalendarEvent.HolidayTip -> tip = Tip.Holidays(event.holidays)

                CalendarEvent.WeekendTip -> tip = Tip.Weekend

                is CalendarEvent.AnchorTip -> tip = null
            }
        }
    }
    LaunchedEffect(tip) {
        if (tip != null) {
            delay(TIP_MS)
            tip = null
        }
    }

    CalendarScreen(
        state = state,
        tip = tip,
        onShowExpectedTip = { tip = Tip.Expected },
        viewModel = viewModel,
        modifier = modifier,
    )
}

// 드래그 중 선택 구간. 화면 상태라 ViewModel에 두지 않는다
@Stable
private class DragSelection {
    var anchor by mutableStateOf<LocalDate?>(null)
        private set
    var end by mutableStateOf<LocalDate?>(null)
        private set

    val active: Boolean get() = anchor != null

    val range: ClosedRange<LocalDate>?
        get() = anchor?.let { a -> end?.let { b -> minOf(a, b)..maxOf(a, b) } }

    fun start(date: LocalDate) {
        anchor = date
        end = date
    }

    fun move(date: LocalDate) {
        end = date
    }

    fun clear() {
        anchor = null
        end = null
    }
}

@Composable
private fun CalendarScreen(
    state: CalendarUiState,
    tip: Tip?,
    onShowExpectedTip: () -> Unit,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()
    val pagerState =
        rememberPagerState(initialPage = pageOf(YearMonth.from(state.today))) { PAGE_COUNT }
    val drag = remember { DragSelection() }
    var yearPickerOpen by remember { mutableStateOf(false) }
    val month = monthOf(pagerState.currentPage)

    // 애니메이션 줄이기면 바로 이동
    fun goTo(target: YearMonth) = scope.launch {
        if (reduceMotion) {
            pagerState.scrollToPage(
                pageOf(target),
            )
        } else {
            pagerState.animateScrollToPage(pageOf(target))
        }
    }

    val selection = drag.range
    val summary = remember(selection, state.leaves, state.holidays, state.allowance) {
        selection?.takeIf {
            it.start != it.endInclusive
        }?.let { state.summarize(it.start, it.endInclusive) }
    }
    val callbacks = rememberGridCallbacks(state, viewModel, drag)

    Column(modifier.fillMaxSize()) {
        // 말풍선은 제목 위에 띄운다. 자리를 차지하지 않아 달력이 밀리지 않음
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TopBar(
                month = month,
                expected = state.isExpected(month.year),
                onTitle = { yearPickerOpen = true },
                onExpected = onShowExpectedTip,
                onToday = { goTo(YearMonth.from(state.today)) },
            )
            Bubble(
                text = bubbleText(summary, state.rangeAnchor, tip),
                warning = summary?.isOver == true,
            )
        }
        WeekdayHeader()
        if (state.isLoaded) {
            HorizontalPager(
                state = pagerState,
                // 드래그 선택 중에는 월 넘김 막기
                userScrollEnabled = !drag.active,
                modifier = Modifier.weight(1f).fillMaxWidth().testTag(CALENDAR_PAGER_TAG),
            ) { page ->
                MonthPage(
                    month = monthOf(page),
                    state = state,
                    selection = selection ?: state.rangeAnchor?.let { it..it },
                    callbacks = callbacks,
                )
            }
        }
        CoachMarkSlot(visible = state.coachMarkVisible, onDismiss = viewModel::dismissCoachMark)
    }

    DaySheetHost(state, viewModel)
    if (yearPickerOpen) {
        YearPickerSheet(
            current = month.year,
            isExpected = state::isExpected,
            onPick = { year ->
                yearPickerOpen = false
                goTo(YearMonth.of(year, month.monthValue))
            },
            onDismiss = { yearPickerOpen = false },
        )
    }
}

@Composable
private fun rememberGridCallbacks(
    state: CalendarUiState,
    viewModel: CalendarViewModel,
    drag: DragSelection,
): GridCallbacks {
    val haptic = LocalHapticFeedback.current
    return GridCallbacks(
        onTap = { date ->
            val marked = date in state.leaves
            if (marked || state.isWorkday(date)) {
                haptic.performHapticFeedback(
                    if (marked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                )
            }
            viewModel.onTap(date)
        },
        onLongPress = viewModel::openSheet,
        onDragStart = { date ->
            // 모드 전환을 햅틱으로 알림
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            drag.start(date)
        },
        onDragMove = { date ->
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            drag.move(date)
        },
        onDragEnd = { from, to ->
            drag.clear()
            viewModel.commitRange(from, to)
        },
        onDragCancel = drag::clear,
        onRangeFrom = viewModel::rangeFrom,
        onRangeTo = viewModel::rangeTo,
    )
}

@Composable
private fun DaySheetHost(state: CalendarUiState, viewModel: CalendarViewModel) {
    val sheet = state.sheet ?: return
    DaySheet(
        sheet = sheet,
        isWorkday = state.isWorkday(sheet.date),
        leave = state.leaves[sheet.date],
        holidays = state.holidays?.on(sheet.date).orEmpty(),
        hasAnchor = state.rangeAnchor != null,
        actions = DaySheetActions(
            onLeave = viewModel::setLeave,
            onSaveMemo = viewModel::saveMemo,
            onRangeFrom = viewModel::rangeFrom,
            onRangeTo = viewModel::rangeTo,
            onDismiss = viewModel::closeSheet,
        ),
    )
}

@Composable
private fun MonthPage(
    month: YearMonth,
    state: CalendarUiState,
    selection: ClosedRange<LocalDate>?,
    callbacks: GridCallbacks,
) {
    val grid = remember(month, state.holidays, state.leaves) { state.grid(month) } ?: return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 칸 높이는 48dp 이상 유지. 화면이 모자라면 세로 스크롤
        val rowHeight: Dp = maxOf(MIN_ROW, maxHeight / MonthGrid.WEEKS)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MonthGridView(
                grid = grid,
                today = state.today,
                memoDates = state.memoDates,
                selection = selection,
                rowHeight = rowHeight,
                callbacks = callbacks,
            )
        }
    }
}

@Composable
private fun TopBar(
    month: YearMonth,
    expected: Boolean,
    onTitle: () -> Unit,
    onExpected: () -> Unit,
    onToday: () -> Unit,
) {
    val colors = YhTheme.colors
    val title = stringResource(R.string.cal_title_month, month.year, month.monthValue)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.cal_title_hint),
                    onClick = onTitle,
                )
                .padding(horizontal = 8.dp)
                .semantics { heading() }
                .testTag(CALENDAR_TITLE_TAG),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                stringResource(R.string.cal_title_month_picker, month.year, month.monthValue),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = title
                },
            )
        }
        if (expected) {
            // 예상 배지: 글자 + 테두리. 누르면 설명
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onExpected),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.cal_badge_expected),
                    color = colors.holiday,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .border(1.dp, colors.holiday, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Box(Modifier.weight(1f))
        TextButton(
            onClick = onToday,
            modifier = Modifier.heightIn(min = 48.dp).testTag(CALENDAR_TODAY_TAG),
        ) {
            Text(stringResource(R.string.cal_today))
        }
    }
}

@Composable
private fun bubbleText(summary: RangeSummary?, anchor: LocalDate?, tip: Tip?): String? = when {
    summary != null && summary.isOver -> stringResource(
        R.string.cal_bubble_range_over,
        DayCount.format(summary.cost),
        summary.totalDays,
        DayCount.format(summary.overBy),
    )

    summary != null ->
        stringResource(R.string.cal_bubble_range, DayCount.format(summary.cost), summary.totalDays)

    anchor != null -> stringResource(R.string.cal_bubble_anchor, dateLabel(anchor))

    tip is Tip.Holidays -> tip.holidays.map { holidayName(it) }.joinToString()

    tip == Tip.Weekend -> stringResource(R.string.cal_bubble_weekend)

    tip == Tip.Expected -> stringResource(R.string.cal_expected_note)

    else -> null
}

// 드래그 요약·공휴일 이름 말풍선. 초과면 경고색 + 글자
@Composable
private fun Bubble(text: String?, warning: Boolean) {
    val colors = YhTheme.colors
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (text != null) {
            Text(
                text = text,
                color = if (warning) colors.onWarning else colors.background,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .background(
                        if (warning) colors.warning else colors.ink,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag(CALENDAR_BUBBLE_TAG),
            )
        }
    }
}
