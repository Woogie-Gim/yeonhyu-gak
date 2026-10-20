package com.gsw.yeonhyugak.core.engine.holiday

// 원격 파일 비교. 새 공휴일 알림·N 배지용
object RemoteHolidayDiff {
    // 이전 파일에 없던 날. 이전 파일이 없으면 전부 새 날
    fun newDays(old: RemoteHolidays?, new: RemoteHolidays): List<Holiday> {
        val known = old?.holidays.orEmpty().map { it.date to it.name }.toSet()
        return new.holidays.filter { (it.date to it.name) !in known }
    }

    // 개정 번호와 내용이 모두 같으면 무시
    fun isSame(old: RemoteHolidays?, new: RemoteHolidays): Boolean =
        old != null && old.version == new.version && old.holidays == new.holidays
}
