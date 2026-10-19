package com.gsw.yeonhyugak.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gsw.yeonhyugak.core.ui.theme.YhTheme
import com.gsw.yeonhyugak.core.ui.theme.numeralText

const val STEPPER_MINUS_TAG = "stepper_minus"
const val STEPPER_PLUS_TAG = "stepper_plus"
const val STEPPER_VALUE_TAG = "stepper_value"

// 문구는 호출한 쪽 strings.xml에서 넘긴다
data class StepperLabels(val value: String, val minus: String, val plus: String)

// 큰 스테퍼. 0.5 단위. 버튼은 엄지로 누르기 쉽게 64dp
@Composable
fun DayStepper(
    value: Double,
    onValueChange: (Double) -> Unit,
    labels: StepperLabels,
    modifier: Modifier = Modifier,
    step: Double = STEP,
    range: ClosedFloatingPointRange<Double> = 0.0..MAX_DAYS,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FilledTonalButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = value > range.start,
            modifier = Modifier
                .size(BUTTON)
                .semantics { contentDescription = labels.minus }
                .testTag(STEPPER_MINUS_TAG),
        ) { Text("−", style = MaterialTheme.typography.headlineSmall) }
        Text(
            text = YhTheme.fonts.numeralText(labels.value),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = YhTheme.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 120.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(STEPPER_VALUE_TAG),
        )
        FilledTonalButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = value < range.endInclusive,
            modifier = Modifier
                .size(BUTTON)
                .semantics { contentDescription = labels.plus }
                .testTag(STEPPER_PLUS_TAG),
        ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
    }
}

private const val STEP = 0.5
private const val MAX_DAYS = 60.0
private val BUTTON = 64.dp
