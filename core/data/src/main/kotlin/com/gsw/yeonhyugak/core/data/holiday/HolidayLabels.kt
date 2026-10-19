package com.gsw.yeonhyugak.core.data.holiday

import androidx.annotation.StringRes
import com.gsw.yeonhyugak.core.data.R
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import com.gsw.yeonhyugak.core.engine.holiday.HolidayKey

// KASI 이름 → 화면 표시 이름. 키 없는 선거일·임시공휴일은 원문 그대로
object HolidayLabels {
    @StringRes
    fun resOf(key: HolidayKey): Int = when (key) {
        HolidayKey.NEW_YEAR -> R.string.holiday_new_year
        HolidayKey.SEOLLAL -> R.string.holiday_seollal
        HolidayKey.INDEPENDENCE -> R.string.holiday_independence
        HolidayKey.LABOR -> R.string.holiday_labor
        HolidayKey.CHILDREN -> R.string.holiday_children
        HolidayKey.BUDDHA -> R.string.holiday_buddha
        HolidayKey.MEMORIAL -> R.string.holiday_memorial
        HolidayKey.CONSTITUTION -> R.string.holiday_constitution
        HolidayKey.LIBERATION -> R.string.holiday_liberation
        HolidayKey.CHUSEOK -> R.string.holiday_chuseok
        HolidayKey.FOUNDATION -> R.string.holiday_foundation
        HolidayKey.HANGUL -> R.string.holiday_hangul
        HolidayKey.CHRISTMAS -> R.string.holiday_christmas
    }

    // getString은 Context::getString 형태 (리소스 ID, 포맷 인자)
    fun displayName(holiday: Holiday, getString: (Int, Array<out Any>) -> String): String {
        val parsed = holiday.parsedName
        val key = parsed.key
        val origin = parsed.substituteOf
        return when {
            key != null -> getString(resOf(key), emptyArray())

            origin != null -> getString(
                R.string.holiday_substitute_of,
                arrayOf(getString(resOf(origin), emptyArray())),
            )

            parsed.isSubstitute -> getString(R.string.holiday_substitute, emptyArray())

            else -> holiday.name
        }
    }
}
