package com.gsw.yeonhyugak.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
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
import com.gsw.yeonhyugak.core.ui.rememberNotificationPermission
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
const val SETTINGS_NOTIFY_TAG = "settings_notify"
const val SETTINGS_QR_TAG = "settings_qr"
const val SETTINGS_DEV_TAG = "settings_dev"

// 디버그 빌드에서만 넘어오는 줄. 문구도 호출한 쪽(디버그 소스)에서 준다
class SettingsLink(val label: String, val onClick: () -> Unit)

fun settingsThemeTag(id: ThemeId): String = "settings_theme_${id.name.lowercase()}"

fun settingsSoundPreviewTag(id: ThemeId): String = "settings_sound_${id.name.lowercase()}"

fun settingsCheerTag(cheer: CheerColor): String = "settings_cheer_${cheer.name.lowercase()}"

fun settingsSoundModeTag(mode: SoundMode): String = "settings_sound_mode_${mode.name.lowercase()}"

// 테마 · 효과음 · 햅틱 · 알림 · 공유 카드. 나머지 설정은 Phase 6
@Composable
fun SettingsRoute(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    devMenu: SettingsLink? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val player = LocalSfxPlayer.current
    val muted = stringResource(R.string.settings_sound_muted)
    val denied = stringResource(R.string.settings_notify_denied)
    val permission = rememberNotificationPermission()
    SettingsUndoEffect(viewModel, snackbarHostState)

    SettingsScreen(
        state = state,
        actions = SettingsActions(
            onBack = onBack,
            onTheme = viewModel::selectTheme,
            onCheer = viewModel::selectCheer,
            onSound = viewModel::setSound,
            onHaptic = viewModel::setHaptic,
            onNotify = { on ->
                if (!on) {
                    viewModel.setNotify(false)
                } else {
                    permission.request { granted ->
                        viewModel.setNotify(granted)
                        if (!granted) {
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(denied)
                            }
                        }
                    }
                }
            },
            onShareQr = viewModel::setShareQr,
            devMenu = devMenu,
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
    val onNotify: (Boolean) -> Unit = {},
    val onShareQr: (Boolean) -> Unit = {},
    val devMenu: SettingsLink? = null,
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
        item { SectionTitle(R.string.settings_notify_section) }
        item {
            ToggleRow(
                R.string.settings_notify,
                R.string.settings_notify_desc,
                state.notifyOn,
                actions.onNotify,
                SETTINGS_NOTIFY_TAG,
            )
        }
        item { SectionTitle(R.string.settings_share_section) }
        item {
            ToggleRow(
                R.string.settings_share_qr,
                R.string.settings_share_qr_desc,
                state.shareQr,
                actions.onShareQr,
                SETTINGS_QR_TAG,
            )
        }
        actions.devMenu?.let { link -> item { LinkRow(link) } }
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

@Composable
private fun LinkRow(link: SettingsLink) {
    Text(
        link.label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = link.onClick)
            .testTag(SETTINGS_DEV_TAG)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    )
}

private val BOTTOM_SPACE = 96.dp
