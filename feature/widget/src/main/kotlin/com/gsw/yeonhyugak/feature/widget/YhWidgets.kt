package com.gsw.yeonhyugak.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.widget.WidgetRefresher
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun loader(): WidgetSnapshotLoader
}

private suspend fun Context.snapshot(): WidgetSnapshot =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
        .loader()
        .load()

class NextBreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = context.snapshot()
        provideContent { NextBreakContent(snapshot) }
    }
}

class MonthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = context.snapshot()
        provideContent { MonthContent(snapshot) }
    }
}

class NextBreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextBreakWidget()
}

class MonthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthWidget()
}

class GlanceWidgetRefresher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : WidgetRefresher {
    override suspend fun refresh() {
        NextBreakWidget().updateAll(context)
        MonthWidget().updateAll(context)
    }
}

// 연차 지정·해제·Undo는 모두 leave_day 테이블을 거친다 → 그 변화만 보고 갱신
@Singleton
class LeaveWidgetSync @Inject constructor(
    private val leaves: LeaveRepository,
    private val widgets: WidgetRefresher,
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        leaves.observe().distinctUntilChanged().drop(1).collect { widgets.refresh() }
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface WidgetModule {
    @Binds
    fun refresher(impl: GlanceWidgetRefresher): WidgetRefresher
}
