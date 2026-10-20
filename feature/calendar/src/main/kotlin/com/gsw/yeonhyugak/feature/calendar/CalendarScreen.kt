package com.gsw.yeonhyugak.feature.calendar

import androidx.annotation.StringRes
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.engine.calendar.MonthGrid
import com.gsw.yeonhyugak.core.engine.calendar.RangeSummary
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.ui.DayCount
import com.gsw.yeonhyugak.core.ui.MonthBackdrop
import com.gsw.yeonhyugak.core.ui.R as CoreUiR
import com.gsw.yeonhyugak.core.ui.monthFlingBehavior
import com.gsw.yeonhyugak.core.ui.monthTransition
import com.gsw.yeonhyugak.core.ui.rememberReduceMotion
import com.gsw.yeonhyugak.core.ui.rememberYhFeedback
import com.gsw.yeonhyugak.core.ui.theme.ProvideMonthColors
import com.gsw.yeonhyugak.core.ui.theme.SfxEffect
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.core.ui.theme.numeralText
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
const val CALENDAR_SETTINGS_TAG = "cal_settings"
const val CALENDAR_SHARE_TAG = "cal_share"
const val NOTIFY_ASK_TAG = "cal_notify_ask"

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
internal sealed interface Tip {
    data class Holidays(val holidays: List<Holiday>) : Tip

    data object Weekend : Tip

    data object Expected : Tip
}

// 야구 테마: 연차 지정 문구 앞에 `세이프`
internal fun themedMessageRes(@StringRes res: Int, theme: ThemeId): Int = when {
    theme != ThemeId.BASEBALL -> res
    res == R.string.cal_done_full -> R.string.cal_done_full_baseball
    res == R.string.cal_done_range -> R.string.cal_done_range_baseball
    else -> res
}

@Composable
fun CalendarRoute(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenShare: (year: Int) -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val res = LocalResources.current
    val a11y = LocalAccessibilityManager.current
    var tip by remember { mutableStateOf<Tip?>(null) }
    var askNotify by remember { mutableStateOf(false) }
    val undoLabel = stringResource(R.string.cal_undo)
    val theme = YhTheme.spec.id
    val feedback = rememberYhFeedback()
    val currentTheme by rememberUpdatedState(theme)
    val currentFeedback by rememberUpdatedState(feedback)

    LaunchedEffect(viewModel) {
        var undoJob: Job? = null
        viewModel.eventFlow.collect { event ->
            when (event) {
                is CalendarEvent.Changed -> {
                    val done = event.message.res == R.string.cal_done_add
                    if (done) currentFeedback.sound(SfxEffect.SUCCESS)
                    undoJob?.cancel()
                    undoJob = launch {
                        val message = res.getString(
                            themedMessageRes(event.message.res, currentTheme),
                            *event.message.args.toTypedArray(),
                        )
                        val label = if (event.undoable) undoLabel else null
                        if (snackbarHostState.showChange(message, label, a11y)) viewModel.undo()
                    }
                }

                is CalendarEvent.HolidayTip -> tip = Tip.Holidays(event.holidays)

                CalendarEvent.WeekendTip -> tip = Tip.Weekend

                is CalendarEvent.AnchorTip -> tip = null

                CalendarEvent.AskNotify -> askNotify = true
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
        onOpenSettings = onOpenSettings,
        onOpenShare = onOpenShare,
        viewModel = viewModel,
        modifier = modifier,
    )
    if (askNotify) {
        NotifyAskDialog(
            onAnswer = { on ->
                askNotify = false
                viewModel.links.answerNotify(on)
            },
        )
    }
}

// 변경 스낵바 5초. TalkBack 사용자는 권장 시간만큼 더 길게. 되돌리기를 누르면 true
private suspend fun SnackbarHostState.showChange(
    message: String,
    undoLabel: String?,
    a11y: AccessibilityManager?,
): Boolean {
    val timeout = a11y?.calculateRecommendedTimeoutMillis(
        UNDO_MS,
        containsIcons = false,
        containsText = true,
        containsControls = undoLabel != null,
    ) ?: UNDO_MS
    currentSnackbarData?.dismiss()
    val result = withTimeoutOrNull(timeout) {
        showSnackbar(
            message = message,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Indefinite,
        )
    }
    return result == SnackbarResult.ActionPerformed
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
    onOpenSettings: () -> Unit,
    onOpenShare: (year: Int) -> Unit,
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
    PageSoundEffect(pagerState)

    // 애니메이션 줄이기면 바로 이동
    fun goTo(target: YearMonth) = scope.launch {
        val page = pageOf(target)
        if (reduceMotion) pagerState.scrollToPage(page) else pagerState.animateScrollToPage(page)
    }

    val selection = drag.range
    val summary = rememberRangeSummary(selection, state)
    val callbacks = rememberGridCallbacks(state, viewModel, drag)

    // 추천 카드에서 오면 미리보기 첫 날짜가 있는 달로 이동
    LaunchedEffect(state.preview?.focus) {
        state.preview?.focus?.let { goTo(YearMonth.from(it)) }
    }

    OpenDateEffect(viewModel.links) { goTo(it) }

    Column(modifier.fillMaxSize()) {
        // 말풍선은 제목 위에 띄운다. 자리를 차지하지 않아 달력이 밀리지 않음
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TopBar(
                month = month,
                expected = state.isExpected(month.year),
                onTitle = { yearPickerOpen = true },
                onExpected = onShowExpectedTip,
                onToday = { goTo(YearMonth.from(state.today)) },
                onSettings = onOpenSettings,
                onShare = { onOpenShare(month.year) },
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
                flingBehavior = monthFlingBehavior(pagerState, reduceMotion),
                modifier = Modifier.weight(1f).fillMaxWidth().testTag(CALENDAR_PAGER_TAG),
            ) { page ->
                MonthPlate(pagerState, page, reduceMotion) {
                    MonthPage(
                        month = monthOf(page),
                        state = state,
                        selection = selection ?: state.rangeAnchor?.let { it..it },
                        callbacks = callbacks,
                    )
                }
            }
        }
        BottomSlot(state, viewModel)
    }

    DaySheetHost(state, viewModel)
    if (yearPickerOpen) {
        YearPickerHost(month, state, onDismiss = { yearPickerOpen = false }) { goTo(it) }
    }
}

// 드래그 중 말풍선 요약. 한 칸만 누른 상태는 요약 없음
@Composable
private fun rememberRangeSummary(
    selection: ClosedRange<LocalDate>?,
    state: CalendarUiState,
): RangeSummary? = remember(selection, state.leaves, state.holidays, state.allowance) {
    selection?.takeIf { it.start != it.endInclusive }
        ?.let { state.summarize(it.start, it.endInclusive) }
}

@Composable
private fun rememberGridCallbacks(
    state: CalendarUiState,
    viewModel: CalendarViewModel,
    drag: DragSelection,
): GridCallbacks {
    val feedback = rememberYhFeedback()
    // 미리보기 중에는 지정하지 않는다. 월 넘김만 허용
    if (state.preview != null) return PreviewOnlyCallbacks
    return GridCallbacks(
        onTap = { date ->
            val marked = date in state.leaves
            if (marked || state.isWorkday(date)) {
                feedback.haptic(
                    if (marked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                )
                feedback.sound(if (marked) SfxEffect.UNMARK else SfxEffect.MARK)
            }
            viewModel.onTap(date)
        },
        onLongPress = viewModel::openSheet,
        onDragStart = { date ->
            // 모드 전환을 햅틱으로 알림
            feedback.haptic(HapticFeedbackType.LongPress)
            drag.start(date)
        },
        onDragMove = { date ->
            feedback.haptic(HapticFeedbackType.SegmentFrequentTick)
            drag.move(date)
        },
        onDragEnd = { from, to ->
            drag.clear()
            feedback.sound(SfxEffect.MARK)
            viewModel.commitRange(from, to)
        },
        onDragCancel = drag::clear,
        onRangeFrom = viewModel::rangeFrom,
        onRangeTo = viewModel::rangeTo,
    )
}

private val PreviewOnlyCallbacks = GridCallbacks(
    onTap = {},
    onLongPress = {},
    onDragStart = {},
    onDragMove = {},
    onDragEnd = { _, _ -> },
    onDragCancel = {},
    onRangeFrom = {},
    onRangeTo = {},
)

// 연도 고르면 같은 달의 그 연도로
@Composable
private fun YearPickerHost(
    month: YearMonth,
    state: CalendarUiState,
    onDismiss: () -> Unit,
    onGo: (YearMonth) -> Unit,
) {
    YearPickerSheet(
        current = month.year,
        isExpected = state::isExpected,
        onPick = { year ->
            onDismiss()
            onGo(YearMonth.of(year, month.monthValue))
        },
        onDismiss = onDismiss,
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
    val grid =
        remember(month, state.holidays, state.leaves, state.preview) { state.grid(month) } ?: return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 칸 높이는 48dp 이상 유지. 화면이 모자라면 세로 스크롤
        val rowHeight: Dp = maxOf(MIN_ROW, maxHeight / MonthGrid.WEEKS)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MonthGridView(
                grid = grid,
                today = state.today,
                memoDates = state.memoDates,
                newHolidayDates = state.newHolidayDates,
                selection = selection,
                rowHeight = rowHeight,
                callbacks = callbacks,
            )
        }
    }
}
