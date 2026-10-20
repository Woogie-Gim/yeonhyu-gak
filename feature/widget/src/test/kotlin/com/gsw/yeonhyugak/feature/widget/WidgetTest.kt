package com.gsw.yeonhyugak.feature.widget

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.holiday.NoHolidayOverrides
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.data.widget.WidgetRefresher
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class WidgetTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, YhDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val leaves = LeaveRepository(db)
    private val prefs = UserPrefsRepository(
        PreferenceDataStoreFactory.create {
            File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
        },
    )
    private val holidays = HolidayRepository(context, NoHolidayOverrides(), Dispatchers.Unconfined)

    @After
    fun tearDown() = db.close()

    // UTC 4/19 15:30 = 서울 4/20 00:30
    private fun loader(utc: String = "2026-04-19T15:30:00Z") = WidgetSnapshotLoader(
        holidays,
        leaves,
        prefs,
        Clock.fixed(Instant.parse(utc), ZoneOffset.UTC),
    )

    @Test
    fun snapshot_seoulTodayNextBreakRemaining() = runBlocking {
        prefs.setAllowance(15.0)
        leaves.mark(listOf(LocalDate.of(2026, 4, 30)), LeaveType.FULL)
        val s = loader().load()
        assertEquals(LocalDate.of(2026, 4, 20), s.today)
        // 4/30 연차 + 5/1 노동절 + 주말 → 4/30~5/3
        assertEquals(LocalDate.of(2026, 4, 30), s.next?.span?.start)
        assertEquals(10, s.next?.daysLeft)
        assertEquals("노동절", s.nextHoliday?.name)
        assertEquals(14.0, s.remaining, 0.0)
    }

    @Test
    fun nextBreak_showsDdayAndName() = runGlanceAppWidgetUnitTest {
        val s = runBlocking { loader().load() }
        setContext(context)
        provideComposable { NextBreakContent(s) }
        onNode(hasText("다음 연휴까지")).assertExists()
        onNode(hasText("D-11")).assertExists()
        onNode(hasText("노동절 · 3일")).assertExists()
    }

    @Test
    fun nextBreak_ongoing() = runGlanceAppWidgetUnitTest {
        // 서울 5/2(토)
        val s = runBlocking { loader("2026-05-01T15:30:00Z").load() }
        setContext(context)
        provideComposable { NextBreakContent(s) }
        onNode(hasText("지금 연휴 중이에요")).assertExists()
    }

    @Test
    fun month_showsTitleAndRemaining() = runGlanceAppWidgetUnitTest {
        runBlocking {
            prefs.setAllowance(15.0)
            leaves.mark(listOf(LocalDate.of(2026, 4, 21)), LeaveType.AM_HALF)
        }
        val s = runBlocking { loader().load() }
        setContext(context)
        provideComposable { MonthContent(s) }
        onNode(hasText("4월")).assertExists()
        onNode(hasText("남은 연차 14.5일")).assertExists()
    }

    @Test
    fun leaveChanges_refreshWidgets() = runBlocking {
        val count = AtomicInteger()
        val scope = CoroutineScope(Dispatchers.Default)
        LeaveWidgetSync(leaves, WidgetRefresher { count.incrementAndGet() }).start(scope)
        val day = LocalDate.of(2026, 5, 4)
        try {
            delay(SETTLE_MS)
            assertEquals(0, count.get())

            leaves.mark(listOf(day), LeaveType.FULL)
            awaitCount(count, 1)
            val snapshot = leaves.clear(listOf(day))
            awaitCount(count, 2)
            // Undo
            leaves.restore(snapshot)
            awaitCount(count, 3)
        } finally {
            scope.cancel()
        }
    }

    private suspend fun awaitCount(count: AtomicInteger, expected: Int) {
        withTimeout(TIMEOUT_MS) { while (count.get() < expected) delay(POLL_MS) }
        assertEquals(expected, count.get())
    }

    private companion object {
        const val SETTLE_MS = 300L
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 20L
    }
}
