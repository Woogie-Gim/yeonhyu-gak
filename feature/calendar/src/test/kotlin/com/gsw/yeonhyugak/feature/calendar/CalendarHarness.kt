package com.gsw.yeonhyugak.feature.calendar

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.memo.MemoRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.holiday.HolidayOverrides
import com.gsw.yeonhyugak.core.engine.holiday.HolidaySource
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

// 실제 번들 공휴일 + 메모리 DB + 임시 DataStore. 오늘 = 2026-05-06(수)
class CalendarHarness(coachMarkShown: Boolean = true) {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(context, YhDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val prefsFile = File(context.filesDir, "prefs_${UUID.randomUUID()}.preferences_pb")
    private val clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    val leaves = LeaveRepository(db)
    val memos = MemoRepository(db, clock)
    val prefs = UserPrefsRepository(PreferenceDataStoreFactory.create { prefsFile })
    private val holidays = HolidayRepository(
        context,
        { HolidayOverrides(add = listOf(REMOTE_ELECTION)) },
        Dispatchers.Unconfined,
    )

    init {
        if (coachMarkShown) runBlocking { prefs.markCoachMarkShown() }
    }

    // ViewModelStore로 만들어 close 때 코루틴까지 정리
    private val store = ViewModelStore()

    fun viewModel(): CalendarViewModel = ViewModelProvider.create(
        store,
        viewModelFactory {
            initializer { CalendarViewModel(holidays, leaves, memos, prefs, clock) }
        },
    )[CalendarViewModel::class]

    fun leaveMap(): Map<LocalDate, LeaveType> = runBlocking { leaves.observe().first() }

    fun memoText(date: LocalDate): String? = runBlocking { memos.text(date) }

    fun close() {
        store.clear()
        db.close()
        prefsFile.delete()
    }

    companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 5, 6)

        // 원격으로 들어온 선거일 (테스트용 가상 날짜)
        val REMOTE_ELECTION = Holiday(
            LocalDate.of(2026, 5, 13),
            "재보궐선거",
            HolidayKind.ELECTION,
            confirmed = true,
            source = HolidaySource.REMOTE,
        )

        fun may(day: Int): LocalDate = LocalDate.of(2026, 5, day)
    }
}
