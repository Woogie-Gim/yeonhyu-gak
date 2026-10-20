package com.gsw.yeonhyugak.feature.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gsw.yeonhyugak.core.data.holiday.SyncResult
import com.gsw.yeonhyugak.core.data.widget.WidgetRefresher
import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// Worker는 Hilt 주입 대신 EntryPoint로 꺼낸다
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AlertsEntryPoint {
    fun alertRunner(): AlertRunner

    fun syncRunner(): HolidaySyncRunner

    fun scheduler(): BackgroundScheduler

    fun widgets(): WidgetRefresher
}

@Module
@InstallIn(SingletonComponent::class)
interface AlertsModule {
    @Binds
    fun sink(impl: AlertNotifier): AlertSink
}

private fun Context.alerts(): AlertsEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, AlertsEntryPoint::class.java)

// 매일 09:00 KST. 끝나면 다음 날 것을 이어 예약
class DailyAlertWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val e = applicationContext.alerts()
        try {
            e.alertRunner().runDaily()
        } finally {
            e.scheduler().scheduleNextDaily()
        }
        return Result.success()
    }
}

// 주 1회. 네트워크 실패만 다시 시도, 스키마 오류는 번들 유지하고 끝
class HolidaySyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (applicationContext.alerts().syncRunner().run()) {
        SyncResult.NetworkError -> Result.retry()
        else -> Result.success()
    }
}

// 자정(KST) 위젯 날짜 갱신
class MidnightWidgetWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val e = applicationContext.alerts()
        try {
            e.widgets().refresh()
        } finally {
            e.scheduler().scheduleNextMidnight()
        }
        return Result.success()
    }
}
