package com.gsw.yeonhyugak

import android.app.Application
import com.gsw.yeonhyugak.core.ui.AndroidDeviceAudio
import com.gsw.yeonhyugak.core.ui.SfxPlayer
import com.gsw.yeonhyugak.core.ui.SoundPoolEngine
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YhApplication : Application() {
    // 효과음은 앱 전체에서 하나. 처음 재생할 때 불러온다
    val sfxPlayer: SfxPlayer by lazy { SfxPlayer(SoundPoolEngine(this), AndroidDeviceAudio(this)) }
}
