package com.gsw.yeonhyugak.feature.alerts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 정확한 알람 없이 WorkManager 지연으로 서울 시각에 맞춘다
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BackgroundSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    // 서울 2026-10-21 00:30, 기기는 뉴욕
    private val clock = Clock.fixed(
        Instant.parse("2026-10-20T15:30:00Z"),
        ZoneId.of("America/New_York"),
    )

    private fun info(name: String): WorkInfo =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(name).get().single()

    @Test
    fun ensureAll_schedulesSeoulTimes() {
        BackgroundScheduler(context, clock).ensureAll()

        assertEquals(
            Duration.ofMinutes(8 * 60 + 30).toMillis(),
            info(BackgroundScheduler.DAILY).initialDelayMillis,
        )
        assertEquals(
            Duration.ofMinutes(23 * 60 + 31).toMillis(),
            info(BackgroundScheduler.MIDNIGHT).initialDelayMillis,
        )
        val sync = info(BackgroundScheduler.SYNC)
        assertEquals(Duration.ofDays(7).toMillis(), sync.periodicityInfo?.repeatIntervalMillis)
        assertEquals(NetworkType.CONNECTED, sync.constraints.requiredNetworkType)
    }

    @Test
    fun ensureAll_twice_keepsOne() {
        val s = BackgroundScheduler(context, clock)
        s.ensureAll()
        val first = info(BackgroundScheduler.DAILY).id
        s.ensureAll()
        assertEquals(first, info(BackgroundScheduler.DAILY).id)
    }
}
