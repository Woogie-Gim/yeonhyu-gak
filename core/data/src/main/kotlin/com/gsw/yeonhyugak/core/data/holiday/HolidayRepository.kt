package com.gsw.yeonhyugak.core.data.holiday

import android.content.Context
import com.gsw.yeonhyugak.core.data.di.IoDispatcher
import com.gsw.yeonhyugak.core.engine.holiday.HolidayBundleCodec
import com.gsw.yeonhyugak.core.engine.holiday.HolidayCalendar
import com.gsw.yeonhyugak.core.engine.holiday.HolidayOverrides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// 원격 덮어쓰기 공급원. 앱은 StoredHolidayOverrides
fun interface HolidayOverrideSource {
    suspend fun current(): HolidayOverrides
}

class NoHolidayOverrides @Inject constructor() : HolidayOverrideSource {
    override suspend fun current(): HolidayOverrides = HolidayOverrides.NONE
}

// 번들 공휴일 + 음력표 + 원격 덮어쓰기를 한 번 병합해 캐시. 원격 갱신 시 invalidate
@Singleton
class HolidayRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val overrides: HolidayOverrideSource,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private var cached: HolidayCalendar? = null
    private val revision = MutableStateFlow(0)

    suspend fun calendar(): HolidayCalendar = mutex.withLock {
        cached ?: withContext(io) { load() }.also { cached = it }
    }

    // 갱신될 때마다 새 달력을 다시 내보낸다
    fun observe(): Flow<HolidayCalendar> = revision.map { calendar() }

    suspend fun invalidate() {
        mutex.withLock { cached = null }
        revision.update { it + 1 }
    }

    private suspend fun load(): HolidayCalendar = HolidayCalendar(
        bundle = HolidayBundleCodec.parseBundle(asset(BUNDLE)),
        lunar = HolidayBundleCodec.parseLunarTable(asset(LUNAR)),
        overrides = overrides.current(),
    )

    private fun asset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private companion object {
        const val BUNDLE = "holidays/kr_holidays.json"
        const val LUNAR = "holidays/lunar_table.json"
    }
}
