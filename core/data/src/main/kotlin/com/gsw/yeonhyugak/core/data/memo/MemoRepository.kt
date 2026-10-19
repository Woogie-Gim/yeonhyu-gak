package com.gsw.yeonhyugak.core.data.memo

import com.gsw.yeonhyugak.core.data.db.MemoEntity
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 변경 전 메모. null = 메모 없음
data class MemoSnapshot(val date: LocalDate, val previous: String?)

@Singleton
class MemoRepository @Inject constructor(private val db: YhDatabase, private val clock: Clock) {
    private val dao get() = db.memoDao()

    // 달력 점 표시용. 메모 내용은 내보내지 않는다
    fun observeDates(): Flow<Set<LocalDate>> = dao.observeDates().map { it.toSet() }

    suspend fun text(date: LocalDate): String? = dao.find(date)?.text

    // 빈 글은 삭제로 처리
    suspend fun save(date: LocalDate, text: String): MemoSnapshot {
        val snapshot = MemoSnapshot(date, text(date))
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            dao.delete(date)
        } else {
            dao.upsert(MemoEntity(date, trimmed, clock.millis()))
        }
        return snapshot
    }

    suspend fun delete(date: LocalDate): MemoSnapshot = save(date, "")

    suspend fun restore(snapshot: MemoSnapshot) {
        val previous = snapshot.previous
        if (previous == null) {
            dao.delete(snapshot.date)
        } else {
            dao.upsert(MemoEntity(snapshot.date, previous, clock.millis()))
        }
    }
}
