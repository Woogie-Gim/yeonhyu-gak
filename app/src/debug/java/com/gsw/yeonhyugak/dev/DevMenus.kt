package com.gsw.yeonhyugak.dev

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gsw.yeonhyugak.R
import com.gsw.yeonhyugak.core.data.holiday.SyncResult
import com.gsw.yeonhyugak.core.data.widget.WidgetRefresher
import com.gsw.yeonhyugak.feature.alerts.AlertRunner
import com.gsw.yeonhyugak.feature.alerts.HolidaySyncRunner
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// 디버그 빌드: 개발자 메뉴 있음
object DevMenus {
    val entry: DevMenu? = DebugDevMenu
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DevEntryPoint {
    fun alertRunner(): AlertRunner

    fun syncRunner(): HolidaySyncRunner

    fun widgets(): WidgetRefresher
}

private object DebugDevMenu : DevMenu {
    @Composable
    override fun label(): String = stringResource(R.string.dev_menu_title)

    @Composable
    override fun Screen(onBack: () -> Unit) {
        val context = LocalContext.current
        val actions = remember(context) { realActions(context) }
        DevMenuScreen(actions, onBack)
    }
}

// 버튼마다 실제 작업을 바로 실행하고 결과 문구를 돌려준다. 의존성은 누를 때 꺼낸다
private fun realActions(context: Context): DevActions {
    val e by lazy {
        EntryPointAccessors.fromApplication(context.applicationContext, DevEntryPoint::class.java)
    }
    return DevActions(
        sendAlerts = {
            val sent = e.alertRunner().runDaily(force = true)
            if (sent > 0) {
                context.getString(R.string.dev_alerts_done, sent)
            } else {
                context.getString(R.string.dev_alerts_test)
            }
        },
        syncHolidays = {
            when (val r = e.syncRunner().run()) {
                is SyncResult.Updated -> context.getString(
                    R.string.dev_sync_updated,
                    r.newDays.size,
                )

                SyncResult.Unchanged -> context.getString(R.string.dev_sync_unchanged)

                SyncResult.Invalid -> context.getString(R.string.dev_sync_invalid)

                SyncResult.NetworkError -> context.getString(R.string.dev_sync_network)
            }
        },
        refreshWidgets = {
            e.widgets().refresh()
            context.getString(R.string.dev_widgets_done)
        },
    )
}
