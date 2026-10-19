package com.gsw.yeonhyugak.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.gsw.yeonhyugak.core.engine.leave.LeaveType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

// 연차 기록. 공유 카드는 이 테이블만 읽는다
@Entity(tableName = "leave_day")
data class LeaveEntity(@PrimaryKey val date: LocalDate, val type: LeaveType)

// 날짜 메모. 연차와 분리 저장 (공유 카드 기본 제외)
@Entity(tableName = "day_memo")
data class MemoEntity(
    @PrimaryKey val date: LocalDate,
    val text: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_day")
    fun observeAll(): Flow<List<LeaveEntity>>

    @Query("SELECT * FROM leave_day WHERE date IN (:dates)")
    suspend fun find(dates: List<LocalDate>): List<LeaveEntity>

    @Upsert
    suspend fun upsert(items: List<LeaveEntity>)

    @Query("DELETE FROM leave_day WHERE date IN (:dates)")
    suspend fun delete(dates: List<LocalDate>)
}

@Dao
interface MemoDao {
    // 달력은 날짜만 읽는다. 내용은 시트에서만
    @Query("SELECT date FROM day_memo")
    fun observeDates(): Flow<List<LocalDate>>

    @Query("SELECT * FROM day_memo WHERE date = :date")
    suspend fun find(date: LocalDate): MemoEntity?

    @Upsert
    suspend fun upsert(item: MemoEntity)

    @Query("DELETE FROM day_memo WHERE date = :date")
    suspend fun delete(date: LocalDate)
}

// LocalDate ↔ epochDay
class DateConverters {
    @TypeConverter
    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun fromEpochDay(day: Long): LocalDate = LocalDate.ofEpochDay(day)
}

@Database(entities = [LeaveEntity::class, MemoEntity::class], version = 1, exportSchema = false)
@TypeConverters(DateConverters::class)
abstract class YhDatabase : RoomDatabase() {
    abstract fun leaveDao(): LeaveDao

    abstract fun memoDao(): MemoDao

    companion object {
        const val NAME = "yeonhyugak.db"
    }
}
