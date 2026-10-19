package com.gsw.yeonhyugak.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.engine.appearance.SoundMode
import com.gsw.yeonhyugak.core.ui.theme.YhTheme

internal data class SoundOption(
    val mode: SoundMode,
    @param:StringRes val label: Int,
    @param:StringRes val desc: Int,
)

internal val SoundOptions = listOf(
    SoundOption(
        SoundMode.SYSTEM,
        R.string.settings_sound_system,
        R.string.settings_sound_system_desc,
    ),
    SoundOption(SoundMode.ON, R.string.settings_sound_on, R.string.settings_sound_on_desc),
    SoundOption(SoundMode.OFF, R.string.settings_sound_off, R.string.settings_sound_off_desc),
)

// 효과음: 시스템 따르기 · 켜기 · 끄기. 설명은 한 줄
@Composable
internal fun SoundRow(option: SoundOption, selected: Boolean, onSelect: (SoundMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton) { onSelect(option.mode) }
            .testTag(settingsSoundModeTag(option.mode))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp)) {
            Text(stringResource(option.label), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(option.desc),
                style = MaterialTheme.typography.bodySmall,
                color = YhTheme.colors.muted,
            )
        }
    }
}

@Composable
internal fun HapticRow(on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .testTag(SETTINGS_HAPTIC_TAG)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_haptic),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(R.string.settings_haptic_desc),
                style = MaterialTheme.typography.bodySmall,
                color = YhTheme.colors.muted,
            )
        }
        Switch(checked = on, onCheckedChange = null)
    }
}
