package com.gsw.yeonhyugak.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import com.gsw.yeonhyugak.ui.theme.YhTheme

const val APP_ROOT_TAG = "yh_root"

// 앱 골격. 탭·화면은 Phase 2부터
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun YhApp(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag(APP_ROOT_TAG),
        containerColor = YhTheme.colors.background,
        contentColor = YhTheme.colors.ink,
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun YhAppPreview() {
    YhTheme {
        YhApp()
    }
}
