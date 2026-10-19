package com.gsw.yeonhyugak.core.engine.holiday

// 공휴일 고유 키. kasiName은 특일정보 API 표기 그대로. 화면 표시 이름은 앱 리소스에서
enum class HolidayKey(val kasiName: String) {
    NEW_YEAR("1월1일"),
    SEOLLAL("설날"),
    INDEPENDENCE("삼일절"),
    LABOR("노동절"),
    CHILDREN("어린이날"),
    BUDDHA("부처님오신날"),
    MEMORIAL("현충일"),
    CONSTITUTION("제헌절"),
    LIBERATION("광복절"),
    CHUSEOK("추석"),
    FOUNDATION("개천절"),
    HANGUL("한글날"),
    CHRISTMAS("기독탄신일"),
    ;

    companion object {
        private val byName = entries.associateBy { it.kasiName }

        // 선거일·임시공휴일은 null
        fun fromKasiName(name: String): HolidayKey? = byName[name]
    }
}

// KASI 이름 해석 결과. 대체공휴일이면 substituteOf에 원래 공휴일
data class HolidayName(
    val key: HolidayKey?,
    val isSubstitute: Boolean,
    val substituteOf: HolidayKey?,
)

object HolidayNames {
    const val SUBSTITUTE = "대체공휴일"

    // "대체공휴일" · "대체공휴일(기독탄신일)" · "기독탄신일" · 그 외
    fun parse(name: String): HolidayName {
        if (!name.startsWith(SUBSTITUTE)) {
            return HolidayName(HolidayKey.fromKasiName(name), false, null)
        }
        val origin = name.removePrefix(SUBSTITUTE).removeSurrounding("(", ")")
        return HolidayName(null, true, HolidayKey.fromKasiName(origin))
    }
}
