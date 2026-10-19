package com.gsw.yeonhyugak.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.memo.MemoRepository
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 연차·메모 분리 저장과 Undo 스냅샷
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class StorageTest {
    private lateinit var db: YhDatabase
    private lateinit var leaves: LeaveRepository
    private lateinit var memos: MemoRepository
    private val may7 = LocalDate.of(2026, 5, 7)
    private val may8 = LocalDate.of(2026, 5, 8)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, YhDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        leaves = LeaveRepository(db)
        memos = MemoRepository(db, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun leave_markAndRestore() = runTest {
        leaves.mark(listOf(may7), LeaveType.AM_HALF)
        val snapshot = leaves.mark(listOf(may7, may8), LeaveType.FULL)
        assertEquals(
            mapOf(may7 to LeaveType.FULL, may8 to LeaveType.FULL),
            leaves.observe().first(),
        )

        leaves.restore(snapshot)
        assertEquals(mapOf(may7 to LeaveType.AM_HALF), leaves.observe().first())
    }

    @Test
    fun leave_clearAndRestore() = runTest {
        leaves.mark(listOf(may7, may8), LeaveType.FULL)
        val snapshot = leaves.clear(listOf(may7, may8))
        assertTrue(leaves.observe().first().isEmpty())
        leaves.restore(snapshot)
        assertEquals(2, leaves.observe().first().size)
    }

    @Test
    fun memo_separateFromLeave() = runTest {
        leaves.mark(listOf(may7), LeaveType.FULL)
        memos.save(may7, "병원 예약")

        leaves.clear(listOf(may7))
        assertEquals("병원 예약", memos.text(may7))

        leaves.mark(listOf(may7), LeaveType.FULL)
        memos.delete(may7)
        assertEquals(mapOf(may7 to LeaveType.FULL), leaves.observe().first())
    }

    @Test
    fun memo_observeDatesOnly_andUndo() = runTest {
        val first = memos.save(may7, "  여행 짐 싸기 ")
        assertNull(first.previous)
        assertEquals("여행 짐 싸기", memos.text(may7))
        assertEquals(setOf(may7), memos.observeDates().first())

        val edit = memos.save(may7, "바뀐 메모")
        memos.restore(edit)
        assertEquals("여행 짐 싸기", memos.text(may7))

        memos.restore(first)
        assertTrue(memos.observeDates().first().isEmpty())
    }

    @Test
    fun memo_blankSaveDeletes() = runTest {
        memos.save(may7, "메모")
        memos.save(may7, "   ")
        assertNull(memos.text(may7))
    }
}
