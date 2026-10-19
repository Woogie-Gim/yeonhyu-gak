package com.gsw.yeonhyugak.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.ui.MiniMonth
import com.gsw.yeonhyugak.core.ui.R as CoreUiR
import com.gsw.yeonhyugak.core.ui.theme.ProvideMonthColors
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.core.ui.theme.YhThemeSpec
import com.gsw.yeonhyugak.core.ui.theme.cheerSwatch
import java.time.YearMonth

@StringRes
internal fun themeNameRes(id: ThemeId): Int = when (id) {
    ThemeId.NOTE -> R.string.settings_theme_note
    ThemeId.SEASON -> R.string.settings_theme_season
    ThemeId.ANALOG -> R.string.settings_theme_analog
    ThemeId.LOFI -> R.string.settings_theme_lofi
    ThemeId.BASEBALL -> R.string.settings_theme_baseball
}

@StringRes
private fun themeDescRes(id: ThemeId): Int = when (id) {
    ThemeId.NOTE -> R.string.settings_theme_note_desc
    ThemeId.SEASON -> R.string.settings_theme_season_desc
    ThemeId.ANALOG -> R.string.settings_theme_analog_desc
    ThemeId.LOFI -> R.string.settings_theme_lofi_desc
    ThemeId.BASEBALL -> R.string.settings_theme_baseball_desc
}

@StringRes
internal fun cheerNameRes(cheer: CheerColor): Int = when (cheer) {
    CheerColor.RED -> R.string.settings_cheer_red
    CheerColor.BLUE -> R.string.settings_cheer_blue
    CheerColor.NAVY -> R.string.settings_cheer_navy
    CheerColor.ORANGE -> R.string.settings_cheer_orange
    CheerColor.GREEN -> R.string.settings_cheer_green
    CheerColor.BLACK -> R.string.settings_cheer_black
}

// 카드는 그 테마로 그린다. 미리보기는 이번 달 내 실제 달력
@Composable
internal fun ThemeCard(spec: YhThemeSpec, state: SettingsUiState, actions: SettingsActions) {
    val selected = state.appearance.theme == spec.id
    val name = stringResource(themeNameRes(spec.id))
    YhTheme(
        darkTheme = YhTheme.state.dark,
        appearance = state.appearance.copy(theme = spec.id),
        month = state.today.monthValue,
    ) {
        val colors = YhTheme.colors
        val shape = RoundedCornerShape(16.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(shape)
                .background(colors.background)
                // 선택 = 굵은 테두리 + `사용 중` 글자
                .border(
                    if (selected) 3.dp else 1.dp,
                    if (selected) colors.accent else colors.outline,
                    shape,
                )
                .selectable(selected = selected, role = Role.RadioButton) {
                    actions.onTheme(spec.id)
                }
                .testTag(settingsThemeTag(spec.id))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeCardHeader(spec, name, selected) { actions.onPreviewSound(spec) }
            ThemePreview(spec.id, state, name)
            if (spec.id == ThemeId.BASEBALL) CheerChips(state.appearance.cheer, actions.onCheer)
        }
    }
}

@Composable
private fun ThemeCardHeader(
    spec: YhThemeSpec,
    name: String,
    selected: Boolean,
    onPreview: () -> Unit,
) {
    val colors = YhTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                if (selected) {
                    Text(
                        stringResource(R.string.settings_theme_in_use),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onAccent,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(colors.accent, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                stringResource(themeDescRes(spec.id)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        IconButton(
            onClick = onPreview,
            modifier = Modifier.testTag(settingsSoundPreviewTag(spec.id)),
        ) {
            Icon(
                painterResource(CoreUiR.drawable.ic_speaker),
                contentDescription = stringResource(R.string.settings_sound_preview, name),
                tint = colors.ink,
            )
        }
    }
}

@Composable
private fun ThemePreview(id: ThemeId, state: SettingsUiState, name: String) {
    val thisMonth = YearMonth.from(state.today)
    if (id != ThemeId.SEASON) {
        state.grid(thisMonth)?.let { grid ->
            MiniMonth(
                grid = grid,
                today = state.today,
                description = stringResource(
                    R.string.settings_theme_preview,
                    name,
                    thisMonth.monthValue,
                ),
            )
        }
        return
    }
    // 계절: 좌우로 넘겨 다른 달 분위기
    val pager = rememberPagerState(initialPage = thisMonth.monthValue - 1) { MONTHS }
    HorizontalPager(pager, Modifier.fillMaxWidth()) { page ->
        val month = thisMonth.withMonth(page + 1)
        ProvideMonthColors(month.monthValue) {
            state.grid(month)?.let { grid ->
                MiniMonth(
                    grid = grid,
                    today = state.today,
                    description = stringResource(
                        R.string.settings_theme_preview,
                        name,
                        month.monthValue,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheerChips(current: CheerColor, onCheer: (CheerColor) -> Unit) {
    val colors = YhTheme.colors
    Text(
        stringResource(R.string.settings_cheer_title),
        style = MaterialTheme.typography.labelLarge,
        color = colors.ink,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CheerColor.entries.forEach { cheer ->
            // 색 + 이름 글자 (색만으로 구분하지 않음)
            FilterChip(
                selected = cheer == current,
                onClick = { onCheer(cheer) },
                label = { Text(stringResource(cheerNameRes(cheer))) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(14.dp)
                            .background(cheerSwatch(cheer), CircleShape)
                            .border(1.dp, colors.ink, CircleShape),
                    )
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag(settingsCheerTag(cheer)),
            )
        }
    }
}

private const val MONTHS = 12
