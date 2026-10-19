package com.gsw.yeonhyugak.core.data.leave

import androidx.room.withTransaction
import com.gsw.yeonhyugak.core.data.db.LeaveEntity
import com.gsw.yeonhyugak.core.data.db.YhDatabase
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 변경 전 상태. null = 지정 없음. Undo로 그대로 되돌린다
data class LeaveSnapshot(val before: Map<LocalDate, LeaveType?>)

@Singleton
class LeaveRepository @Inject constructor(private val db: YhDatabase) {
    private val dao get() = db.leaveDao()

    fun observe(): Flow<Map<LocalDate, LeaveType>> =
        dao.observeAll().map { list -> list.associate { it.date to it.type } }

    suspend fun mark(dates: Collection<LocalDate>, type: LeaveType): LeaveSnapshot = change(dates) {
        dao.upsert(dates.map { LeaveEntity(it, type) })
    }

    suspend fun clear(dates: Collection<LocalDate>): LeaveSnapshot = change(dates) {
        dao.delete(dates.toList())
    }

    suspend fun restore(snapshot: LeaveSnapshot) = db.withTransaction {
        val (kept, removed) = snapshot.before.entries.partition { it.value != null }
        dao.delete(removed.map { it.key })
        dao.upsert(kept.map { LeaveEntity(it.key, checkNotNull(it.value)) })
    }

    private suspend fun change(
        dates: Collection<LocalDate>,
        block: suspend () -> Unit,
    ): LeaveSnapshot = db.withTransaction {
        val existing = dao.find(dates.toList()).associate { it.date to it.type }
        block()
        LeaveSnapshot(dates.associateWith { existing[it] })
    }
}
