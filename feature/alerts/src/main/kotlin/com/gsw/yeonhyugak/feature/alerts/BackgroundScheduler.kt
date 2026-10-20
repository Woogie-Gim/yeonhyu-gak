package com.gsw.yeonhyugak.feature.alerts

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gsw.yeonhyugak.core.engine.SeoulTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// WorkManager 예약. 정확한 알람 권한 없이 서울 시각 기준 지연으로만 맞춘다
@Singleton
class BackgroundScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    private val work get() = WorkManager.getInstance(context)

    // 앱 시작 시. 이미 예약돼 있으면 그대로 둔다
    fun ensureAll() {
        enqueueDaily(ExistingWorkPolicy.KEEP)
        enqueueMidnight(ExistingWorkPolicy.KEEP)
        val sync = PeriodicWorkRequestBuilder<HolidaySyncWorker>(SYNC_DAYS, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MIN, TimeUnit.MINUTES)
            .build()
        work.enqueueUniquePeriodicWork(SYNC, ExistingPeriodicWorkPolicy.KEEP, sync)
    }

    // 실행 중인 작업 뒤에 이어 붙인다. 자기 자신을 취소하지 않는다
    fun scheduleNextDaily() = enqueueDaily(ExistingWorkPolicy.APPEND_OR_REPLACE)

    fun scheduleNextMidnight() = enqueueMidnight(ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueueDaily(policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<DailyAlertWorker>()
            .setInitialDelay(delayUntil(ALERT_AT))
            .build()
        work.enqueueUniqueWork(DAILY, policy, request)
    }

    // 날짜가 바뀐 뒤 그리도록 1분 여유
    private fun enqueueMidnight(policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<MidnightWidgetWorker>()
            .setInitialDelay(delayUntil(LocalTime.MIDNIGHT).plus(MIDNIGHT_SLACK))
            .build()
        work.enqueueUniqueWork(MIDNIGHT, policy, request)
    }

    private fun delayUntil(at: LocalTime): Duration = SeoulTime.delayUntil(clock, at)

    companion object {
        const val DAILY = "daily_alert"
        const val MIDNIGHT = "midnight_widget"
        const val SYNC = "holiday_sync"
        val ALERT_AT: LocalTime = LocalTime.of(9, 0)
        val MIDNIGHT_SLACK: Duration = Duration.ofMinutes(1)
        private const val SYNC_DAYS = 7L
        private const val BACKOFF_MIN = 30L
    }
}
