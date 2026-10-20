package com.gsw.yeonhyugak.core.data.holiday

import com.gsw.yeonhyugak.core.data.prefs.AlertPrefsRepository
import com.gsw.yeonhyugak.core.engine.SeoulTime
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayBundleCodec
import com.gsw.yeonhyugak.core.engine.holiday.HolidayDataException
import com.gsw.yeonhyugak.core.engine.holiday.RemoteHolidayDiff
import com.gsw.yeonhyugak.core.engine.holiday.RemoteHolidays
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncResult {
    // newDays가 비어도 개정 번호가 바뀌면 Updated
    data class Updated(val newDays: List<Holiday>) : SyncResult

    data object Unchanged : SyncResult

    // 스키마 오류. 저장본·번들 유지
    data object Invalid : SyncResult

    // 네트워크 실패. 저장본·번들 유지
    data object NetworkError : SyncResult
}

// 받기 → 검증 → 비교 → 저장 → 달력 다시 병합
@Singleton
class RemoteHolidaySync @Inject constructor(
    private val fetcher: RemoteHolidayFetcher,
    private val store: RemoteHolidayStore,
    private val holidays: HolidayRepository,
    private val alertPrefs: AlertPrefsRepository,
    private val clock: Clock,
) {
    suspend fun sync(): SyncResult = when (val fetched = fetch()) {
        is Fetched.Ok -> save(fetched.text, fetched.remote)
        is Fetched.Failed -> fetched.result
    }

    private sealed interface Fetched {
        class Ok(val text: String, val remote: RemoteHolidays) : Fetched

        class Failed(val result: SyncResult) : Fetched
    }

    private suspend fun fetch(): Fetched = try {
        val text = fetcher.fetch()
        Fetched.Ok(text, HolidayBundleCodec.parseRemote(text))
    } catch (_: IOException) {
        Fetched.Failed(SyncResult.NetworkError)
    } catch (_: HolidayDataException) {
        Fetched.Failed(SyncResult.Invalid)
    }

    private suspend fun save(text: String, remote: RemoteHolidays): SyncResult {
        val old = store.read()
        if (RemoteHolidayDiff.isSame(old, remote)) return SyncResult.Unchanged
        val newDays = RemoteHolidayDiff.newDays(old, remote)
        store.write(text)
        holidays.invalidate()
        alertPrefs.addNewHolidays(newDays.map { it.date }, SeoulTime.today(clock))
        return SyncResult.Updated(newDays)
    }
}
