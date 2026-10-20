package com.gsw.yeonhyugak.core.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

// 시스템 알림 권한 요청. 테스트는 LocalNotificationPermission으로 가짜를 넣는다
fun interface NotificationPermissionRequester {
    fun request(onResult: (granted: Boolean) -> Unit)
}

val LocalNotificationPermission = staticCompositionLocalOf<NotificationPermissionRequester?> {
    null
}

// Android 13 미만은 권한이 필요 없다
@Composable
fun rememberNotificationPermission(): NotificationPermissionRequester =
    LocalNotificationPermission.current
        ?: if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            remember { NotificationPermissionRequester { it(true) } }
        } else {
            rememberSystemPermission()
        }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun rememberSystemPermission(): NotificationPermissionRequester {
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pending?.invoke(granted)
            pending = null
        }
    return remember(launcher) {
        NotificationPermissionRequester { onResult ->
            pending = onResult
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
