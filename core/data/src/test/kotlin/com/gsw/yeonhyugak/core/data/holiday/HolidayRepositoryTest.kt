package com.gsw.yeonhyugak.core.data.holiday

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.holiday.HolidayOverrides
import com.gsw.yeonhyugak.core.engine.holiday.HolidaySource
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// 번들 assets에서 달력 데이터를 읽는다
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class HolidayRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun repo(source: HolidayOverrideSource = NoHolidayOverrides()) =
        HolidayRepository(context, source, Dispatchers.Unconfined)

    @Test
    fun bundle_loadsKasiElection() = runTest {
        val cal = repo().calendar()
        val election = cal.on(LocalDate.of(2024, 4, 10)).single()
        assertEquals(HolidaySource.BUNDLED, election.source)
        assertTrue(election.confirmed)
        assertFalse(cal.isExpected(2026))
        assertTrue(cal.isExpected(2030))
    }

    @Test
    fun remoteElection_isHolidayLikeBundled() = runTest {
        val remote = Holiday(
            LocalDate.of(2030, 6, 5),
            "전국동시지방선거",
            HolidayKind.ELECTION,
            confirmed = true,
            source = HolidaySource.REMOTE,
        )
        val cal = repo { HolidayOverrides(add = listOf(remote)) }.calendar()
        assertEquals(listOf(remote), cal.on(remote.date))
        assertTrue(cal.isHoliday(remote.date))
    }

    @Test
    fun calendar_isCached() = runTest {
        val r = repo()
        assertSame(r.calendar(), r.calendar())
    }
}
