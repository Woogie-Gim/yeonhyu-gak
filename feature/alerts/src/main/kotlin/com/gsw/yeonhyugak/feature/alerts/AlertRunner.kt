package com.gsw.yeonhyugak.feature.alerts

import com.gsw.yeonhyugak.core.data.holiday.HolidayRepository
import com.gsw.yeonhyugak.core.data.holiday.RemoteHolidaySync
import com.gsw.yeonhyugak.core.data.holiday.SyncResult
import com.gsw.yeonhyugak.core.data.leave.LeaveRepository
import com.gsw.yeonhyugak.core.data.prefs.AlertPrefsRepository
import com.gsw.yeonhyugak.core.data.prefs.UserPrefsRepository
import com.gsw.yeonhyugak.core.data.widget.WidgetRefresher
import com.gsw.yeonhyugak.core.engine.SeoulTime
import com.gsw.yeonhyugak.core.engine.SupportedYears
import com.gsw.yeonhyugak.core.engine.alert.AlertPlanner
import com.gsw.yeonhyugak.core.engine.calendar.YearBreakCalculator
import com.gsw.yeonhyugak.core.engine.leave.LeaveAccrualCalculator
import com.gsw.yeonhyugak.core.engine.planner.WorkCalendar
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// 매일 아침 연휴 D-21 · 연차 소멸 D-30
class AlertRunner @Inject constructor(
    private val holidays: HolidayRepository,
    private val leaves: LeaveRepository,
    private val prefs: UserPrefsRepository,
    private val alertPrefs: AlertPrefsRepository,
    private val sink: AlertSink,
    private val clock: Clock,
) {
    // force = 개발자 메뉴: 동의·발송 기록 무시, 보낼 것이 없으면 시험 알림
    suspend fun runDaily(force: Boolean = false): Int {
        if (!force && (!alertPrefs.notifyOn.first() || !sink.canPost())) return 0
        val today = SeoulTime.today(clock)
        val work = WorkCalendar(holidays = holidays.calendar().holidayDates)
        val marked = leaves.observe().first()
        val planned = (today.year..today.year + 1)
            .filter { it in SupportedYears.range }
            .flatMap { YearBreakCalculator.planned(it, work, marked) }
            .distinct()
        val expiry = prefs.hireInfo.first()?.let {
            LeaveAccrualCalculator.nextExpiry(it.hireDate, it.basis, today)
        }
        val sent = if (force) emptySet() else alertPrefs.alertSentKeys.first()
        val due = AlertPlanner.due(today, planned, expiry, sent)
        due.forEach(sink::post)
        alertPrefs.addAlertSent(due.map { it.key })
        if (force && due.isEmpty()) sink.postTest(today)
        return due.size
    }
}

// 원격 공휴일 받기 + 새 공휴일 알림 + 위젯 갱신
class HolidaySyncRunner @Inject constructor(
    private val sync: RemoteHolidaySync,
    private val alertPrefs: AlertPrefsRepository,
    private val sink: AlertSink,
    private val widgets: WidgetRefresher,
) {
    suspend fun run(): SyncResult {
        val result = sync.sync()
        if (result is SyncResult.Updated) {
            if (result.newDays.isNotEmpty() && alertPrefs.notifyOn.first()) {
                sink.postNewHolidays(result.newDays)
            }
            widgets.refresh()
        }
        return result
    }
}
