package com.gsw.yeonhyugak.core.data.holiday

import com.gsw.yeonhyugak.core.data.R
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKey
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKind
import com.gsw.yeonhyugak.core.engine.holiday.HolidaySource
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HolidayLabelsTest {
    // strings.xml 값과 같은 가짜 리소스
    private val fake: (Int, Array<out Any>) -> String = { id, args ->
        when (id) {
            R.string.holiday_christmas -> "성탄절"
            R.string.holiday_labor -> "노동절"
            R.string.holiday_substitute -> "대체공휴일"
            R.string.holiday_substitute_of -> "대체공휴일(%1\$s)".format(*args)
            else -> "id:$id"
        }
    }

    private fun named(name: String, kind: HolidayKind = HolidayKind.NATIONAL) =
        Holiday(LocalDate.of(2027, 12, 25), name, kind, true, HolidaySource.BUNDLED)

    @Test
    fun christmas_shownAsSeongtanjeol() {
        assertEquals("성탄절", HolidayLabels.displayName(named("기독탄신일"), fake))
    }

    @Test
    fun substituteWithOrigin_usesOriginDisplayName() {
        assertEquals(
            "대체공휴일(성탄절)",
            HolidayLabels.displayName(named("대체공휴일(기독탄신일)", HolidayKind.SUBSTITUTE), fake),
        )
    }

    @Test
    fun plainSubstitute() {
        assertEquals(
            "대체공휴일",
            HolidayLabels.displayName(named("대체공휴일", HolidayKind.SUBSTITUTE), fake),
        )
    }

    @Test
    fun electionWithoutKey_keepsKasiName() {
        assertEquals(
            "전국동시지방선거",
            HolidayLabels.displayName(named("전국동시지방선거", HolidayKind.ELECTION), fake),
        )
    }

    @Test
    fun everyKey_hasDistinctResource() {
        val ids = HolidayKey.entries.map(HolidayLabels::resOf)
        assertEquals(ids.size, ids.toSet().size)
    }
}
