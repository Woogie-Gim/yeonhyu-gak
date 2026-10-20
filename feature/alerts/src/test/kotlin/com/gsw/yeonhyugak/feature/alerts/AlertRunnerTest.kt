package com.gsw.yeonhyugak.feature.alerts

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.holiday.NoHolidayOverrides
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.prefs.AlertPrefsRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.alert.Alert
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 알림 스케줄: Clock 주입, 서울 기준
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AlertRunnerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, YhDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val leaves = LeaveRepository(db)
    private val store = PreferenceDataStoreFactory.create {
        File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
    }
    private val userPrefs = UserPrefsRepository(store)
    private val prefs = AlertPrefsRepository(store)
    private val holidays = HolidayRepository(context, NoHolidayOverrides(), Dispatchers.Unconfined)
    private val sink = FakeSink()

    @After
    fun tearDown() = db.close()

    // UTC 자정 = 서울 09:00
    private fun runner(utc: String) = AlertRunner(
        holidays,
        leaves,
        userPrefs,
        prefs,
        sink,
        Clock.fixed(Instant.parse(utc), ZoneOffset.UTC),
    )

    @Test
    fun breakD21_sentOnceWhenOptedIn() = runTest {
        prefs.setNotify(true)
        // 5/1 노동절(금) ~ 5/5 어린이날(화). 5/4 연차
        leaves.mark(listOf(LocalDate.of(2026, 5, 4)), LeaveType.FULL)

        assertEquals(1, runner("2026-04-10T00:00:00Z").runDaily())
        val alert = sink.posted.single() as Alert.BreakSoon
        assertEquals(LocalDate.of(2026, 5, 1), alert.span.start)
        assertEquals(21, alert.daysLeft)

        // 다음 날 다시 돌아도 같은 알림은 안 보낸다
        assertEquals(0, runner("2026-04-11T00:00:00Z").runDaily())
        assertEquals(1, sink.posted.size)
    }

    @Test
    fun seoulDate_notDeviceDate() = runTest {
        prefs.setNotify(true)
        leaves.mark(listOf(LocalDate.of(2026, 5, 4)), LeaveType.FULL)
        // UTC 4/9 15:30 = 서울 4/10 00:30 → D-21
        assertEquals(1, runner("2026-04-09T15:30:00Z").runDaily())
    }

    @Test
    fun notOptedIn_sendsNothing() = runTest {
        leaves.mark(listOf(LocalDate.of(2026, 5, 4)), LeaveType.FULL)
        assertEquals(0, runner("2026-04-10T00:00:00Z").runDaily())
        assertTrue(sink.posted.isEmpty())
    }

    @Test
    fun noPermission_sendsNothing() = runTest {
        prefs.setNotify(true)
        sink.allowed = false
        leaves.mark(listOf(LocalDate.of(2026, 5, 4)), LeaveType.FULL)
        assertEquals(0, runner("2026-04-10T00:00:00Z").runDaily())
    }

    @Test
    fun force_sendsTestWhenNothingDue() = runTest {
        assertEquals(0, runner("2026-04-10T00:00:00Z").runDaily(force = true))
        assertEquals(listOf(LocalDate.of(2026, 4, 10)), sink.tests)
    }

    class FakeSink : AlertSink {
        var allowed = true
        val posted = mutableListOf<Alert>()
        val newDays = mutableListOf<List<Holiday>>()
        val tests = mutableListOf<LocalDate>()

        override fun canPost() = allowed

        override fun post(alert: Alert) {
            posted += alert
        }

        override fun postNewHolidays(days: List<Holiday>) {
            newDays += days
        }

        override fun postTest(today: LocalDate) {
            tests += today
        }
    }
}
