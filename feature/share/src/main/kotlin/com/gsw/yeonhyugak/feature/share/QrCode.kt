package com.gsw.yeonhyugak.feature.share

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

// 어두운 칸 = dark, 바탕 = light. 다크 모드에서도 밝은 바탕에 어두운 칸으로 그려 인식률 유지
@Composable
fun QrCode(
    text: String,
    dark: Color,
    light: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(text) { QrMatrix.encode(text) }
    Canvas(modifier.semantics { contentDescription = description }) {
        drawRect(light)
        val cell = minOf(size.width / matrix.width, size.height / matrix.height)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(dark, Offset(x * cell, y * cell), Size(cell, cell))
                }
            }
        }
    }
}
