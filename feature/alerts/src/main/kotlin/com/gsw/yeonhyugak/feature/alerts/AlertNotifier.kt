package com.gsw.yeonhyugak.feature.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gsw.yeonhyugak.core.data.holiday.HolidayLabels
import com.gsw.yeonhyugak.core.data.nav.OpenDateStore
import com.gsw.yeonhyugak.core.engine.alert.Alert
import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// 알림을 내보내는 곳. 테스트는 가짜로 바꾼다
interface AlertSink {
    fun canPost(): Boolean

    fun post(alert: Alert)

    fun postNewHolidays(days: List<Holiday>)

    fun postTest(today: LocalDate)
}

// 권한이 없거나 시스템에서 꺼 두면 아무것도 안 한다
@Singleton
class AlertNotifier @Inject constructor(@param:ApplicationContext private val context: Context) :
    AlertSink {
    private val manager = NotificationManagerCompat.from(context)

    override fun canPost(): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && manager.areNotificationsEnabled()
    }

    override fun post(alert: Alert) = when (alert) {
        is Alert.BreakSoon -> show(
            Note(
                CHANNEL_PLAN,
                alert.key,
                context.getString(R.string.alert_break_title, alert.daysLeft),
                context.getString(
                    R.string.alert_break_text,
                    format(alert.span.start),
                    alert.span.days,
                ),
                alert.span.start,
            ),
        )

        is Alert.LeaveExpiring -> show(
            Note(
                CHANNEL_PLAN,
                alert.key,
                context.getString(R.string.alert_expiry_title, alert.daysLeft),
                context.getString(R.string.alert_expiry_text, format(alert.expiresOn)),
                alert.expiresOn,
            ),
        )
    }

    override fun postNewHolidays(days: List<Holiday>) {
        val first = days.minByOrNull { it.date } ?: return
        val name = HolidayLabels.displayName(first) { id, args -> context.getString(id, *args) }
        val text = if (days.size == 1) {
            context.getString(R.string.alert_new_holiday_one, format(first.date), name)
        } else {
            context.getString(
                R.string.alert_new_holiday_many,
                format(first.date),
                name,
                days.size - 1,
            )
        }
        show(
            Note(
                CHANNEL_HOLIDAY,
                "holiday:${first.date}",
                context.getString(R.string.alert_new_holiday_title),
                text,
                first.date,
            ),
        )
    }

    override fun postTest(today: LocalDate) = show(
        Note(
            CHANNEL_PLAN,
            "test",
            context.getString(R.string.alert_test_title),
            context.getString(R.string.alert_test_text),
            today,
        ),
    )

    private class Note(
        val channel: String,
        val key: String,
        val title: String,
        val text: String,
        val open: LocalDate,
    )

    private fun show(note: Note) {
        if (!canPost()) return
        ensureChannels()
        val notification = NotificationCompat.Builder(context, note.channel)
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle(note.title)
            .setContentText(note.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.text))
            .setAutoCancel(true)
            .setContentIntent(openIntent(note.key, note.open))
            .build()
        // notify 바로 앞에서 한 번 더. Android 13 미만은 ContextCompat이 알림 켜짐 여부로 답한다
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            manager.notify(note.key.hashCode(), notification)
        } catch (e: SecurityException) {
            // 확인 직후 권한이 회수된 경우. 알림만 건너뛴다
            Log.w(TAG, "알림 권한 없음", e)
        }
    }

    // 알림 탭 → 그 날짜로 앱 열기
    private fun openIntent(key: String, date: LocalDate): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        launch.putExtra(OpenDateStore.EXTRA_OPEN_DATE, date.toEpochDay())
        return PendingIntent.getActivity(
            context,
            key.hashCode(),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannels() {
        val system = context.getSystemService(NotificationManager::class.java) ?: return
        system.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_PLAN,
                    context.getString(R.string.alert_channel_plan),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.alert_channel_plan_desc) },
                NotificationChannel(
                    CHANNEL_HOLIDAY,
                    context.getString(R.string.alert_channel_holiday),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.alert_channel_holiday_desc) },
            ),
        )
    }

    private fun format(date: LocalDate): String = date.format(DATE)

    private companion object {
        const val TAG = "AlertNotifier"
        const val CHANNEL_PLAN = "plan"
        const val CHANNEL_HOLIDAY = "holiday"
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일(E)", Locale.KOREAN)
    }
}
