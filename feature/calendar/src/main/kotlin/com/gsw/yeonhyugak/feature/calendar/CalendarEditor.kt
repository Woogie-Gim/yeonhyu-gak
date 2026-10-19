package com.gsw.yeonhyugak.feature.calendar

import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.memo.MemoRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.time.LocalDate
import kotlinx.coroutines.flow.first

// 저장 + 되돌리기. 마지막 변경 하나만 되돌린다
internal class CalendarEditor(
    private val leaves: LeaveRepository,
    private val memos: MemoRepository,
    private val prefs: UserPrefsRepository,
) {
    private var pendingUndo: (suspend () -> Unit)? = null

    // 첫 지정에 성공하면 코치마크를 닫고 다시 띄우지 않는다
    suspend fun mark(dates: List<LocalDate>, type: LeaveType) {
        val snapshot = leaves.mark(dates, type)
        pendingUndo = { leaves.restore(snapshot) }
        if (!prefs.coachMarkShown.first()) prefs.markCoachMarkShown()
    }

    suspend fun clear(date: LocalDate) {
        val snapshot = leaves.clear(listOf(date))
        pendingUndo = { leaves.restore(snapshot) }
    }

    // 빈 글이면 삭제
    suspend fun saveMemo(date: LocalDate, text: String) {
        val snapshot = memos.save(date, text)
        pendingUndo = { memos.restore(snapshot) }
    }

    suspend fun undo() {
        val action = pendingUndo ?: return
        pendingUndo = null
        action()
    }
}
