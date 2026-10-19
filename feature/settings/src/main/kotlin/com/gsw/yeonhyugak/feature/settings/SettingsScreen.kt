package com.gsw.yeonhyugak.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.SoundMode
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.ui.LocalSfxPlayer
import com.gsw.yeonhyugak.core.ui.R as CoreUiR
import com.gsw.yeonhyugak.core.ui.showUndo
import com.gsw.yeonhyugak.core.ui.theme.SfxEffect
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.core.ui.theme.YhThemeSpec
import com.gsw.yeonhyugak.core.ui.theme.YhThemes
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val SETTINGS_TAG = "settings_root"
const val SETTINGS_BACK_TAG = "settings_back"
const val SETTINGS_HAPTIC_TAG = "settings_haptic"

fun settingsThemeTag(id: ThemeId): String = "settings_theme_${id.name.lowercase()}"

fun settingsSoundPreviewTag(id: ThemeId): String = "settings_sound_${id.name.lowercase()}"

fun settingsCheerTag(cheer: CheerColor): String = "settings_cheer_${cheer.name.lowercase()}"

fun settingsSoundModeTag(mode: SoundMode): String = "settings_sound_mode_${mode.name.lowercase()}"

// 지금은 테마 · 효과음 · 햅틱만. 나머지 설정은 Phase 6
@Composable
fun SettingsRoute(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val player = LocalSfxPlayer.current
    val muted = stringResource(R.string.settings_sound_muted)
    SettingsUndoEffect(viewModel, snackbarHostState)

    SettingsScreen(
        state = state,
        actions = SettingsActions(
            onBack = onBack,
            onTheme = viewModel::selectTheme,
            onCheer = viewModel::selectCheer,
            onSound = viewModel::setSound,
            onHaptic = viewModel::setHaptic,
            onPreviewSound = { spec ->
                // 무음·진동이면 소리 대신 안내
                if (player != null && !player.preview(spec.soundPack, SfxEffect.MARK)) {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(muted)
                    }
                }
            },
        ),
        modifier = modifier,
    )
}

// 카드 탭 = 즉시 적용 + 되돌리기 스낵바
@Composable
private fun SettingsUndoEffect(viewModel: SettingsViewModel, host: SnackbarHostState) {
    val res = LocalResources.current
    val a11y = LocalAccessibilityManager.current
    val undoLabel = stringResource(R.string.settings_undo)
    LaunchedEffect(viewModel) {
        var undoJob: Job? = null
        viewModel.eventFlow.collect { event ->
            undoJob?.cancel()
            undoJob = launch {
                when (event) {
                    is SettingsEvent.ThemeChanged -> {
                        val name = res.getString(themeNameRes(event.theme))
                        val message = res.getString(R.string.settings_theme_changed, name)
                        if (host.showUndo(
                                message,
                                undoLabel,
                                a11y,
                            )
                        ) {
                            viewModel.undoTheme(event.previous)
                        }
                    }

                    is SettingsEvent.CheerChanged -> {
                        val name = res.getString(cheerNameRes(event.cheer))
                        val message = res.getString(R.string.settings_cheer_changed, name)
                        if (host.showUndo(
                                message,
                                undoLabel,
                                a11y,
                            )
                        ) {
                            viewModel.undoCheer(event.previous)
                        }
                    }
                }
            }
        }
    }
}

internal class SettingsActions(
    val onBack: () -> Unit,
    val onTheme: (ThemeId) -> Unit,
    val onCheer: (CheerColor) -> Unit,
    val onSound: (SoundMode) -> Unit,
    val onHaptic: (Boolean) -> Unit,
    val onPreviewSound: (YhThemeSpec) -> Unit,
)

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    // 아래 여백: 마지막 줄이 되돌리기 스낵바에 가리지 않게
    LazyColumn(
        modifier.fillMaxSize().testTag(SETTINGS_TAG),
        contentPadding = PaddingValues(bottom = BOTTOM_SPACE),
    ) {
        item { Header(actions.onBack) }
        item { SectionTitle(R.string.settings_theme_section) }
        items(YhThemes.all, key = { it.id }) { spec ->
            ThemeCard(spec, state, actions)
        }
        item { SectionTitle(R.string.settings_sound_section) }
        items(SoundOptions, key = { it.mode }) { option ->
            SoundRow(
                option,
                selected = state.appearance.sound == option.mode,
                onSelect = actions.onSound,
            )
        }
        item { HapticRow(state.appearance.haptic, actions.onHaptic) }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag(SETTINGS_BACK_TAG)) {
            Icon(
                painterResource(CoreUiR.drawable.ic_back),
                contentDescription = stringResource(R.string.settings_back),
                tint = YhTheme.colors.ink,
            )
        }
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        color = YhTheme.colors.muted,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

private val BOTTOM_SPACE = 96.dp
