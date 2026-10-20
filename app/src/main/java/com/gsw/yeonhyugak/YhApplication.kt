package com.gsw.yeonhyugak

import android.app.Application
import com.gsw.yeonhyugak.core.ui.AndroidDeviceAudio
import com.gsw.yeonhyugak.core.ui.SfxPlayer
import com.gsw.yeonhyugak.core.ui.SoundPoolEngine
import com.gsw.yeonhyugak.feature.alerts.BackgroundScheduler
import com.gsw.yeonhyugak.feature.widget.LeaveWidgetSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class YhApplication : Application() {
    // 효과음은 앱 전체에서 하나. 처음 재생할 때 불러온다
    val sfxPlayer: SfxPlayer by lazy { SfxPlayer(SoundPoolEngine(this), AndroidDeviceAudio(this)) }

    @Inject
    lateinit var scheduler: BackgroundScheduler

    @Inject
    lateinit var widgetSync: LeaveWidgetSync

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // 알림 09:00 · 위젯 자정 · 원격 공휴일 주 1회. 이미 있으면 그대로
        scheduler.ensureAll()
        // 연차 지정·해제·Undo 때 위젯 갱신
        widgetSync.start(appScope)
    }
}
