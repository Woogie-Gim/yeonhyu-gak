package com.gsw.yeonhyugak.core.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.provider.Settings
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.gsw.yeonhyugak.core.engine.appearance.RingerState
import com.gsw.yeonhyugak.core.engine.appearance.SoundMode
import com.gsw.yeonhyugak.core.engine.appearance.SoundPolicy
import com.gsw.yeonhyugak.core.ui.theme.SfxEffect
import com.gsw.yeonhyugak.core.ui.theme.SoundPack
import com.gsw.yeonhyugak.core.ui.theme.YhTheme

// 실제 소리 재생 (SoundPool). 테스트에서는 가짜로 바꾼다
fun interface SfxEngine {
    fun play(@RawRes res: Int)
}

// 기기 소리 상태
interface DeviceAudio {
    val ringer: RingerState
    val touchSoundsOn: Boolean
}

// 벨소리 모드·설정을 보고 재생 여부를 정한다 (SoundPolicy)
class SfxPlayer(private val engine: SfxEngine, private val audio: DeviceAudio) {
    fun play(pack: SoundPack, effect: SfxEffect, mode: SoundMode) {
        if (SoundPolicy.shouldPlay(
                mode,
                audio.ringer,
                audio.touchSoundsOn,
            )
        ) {
            engine.play(pack.of(effect))
        }
    }

    // 미리 듣기: 사용자가 직접 눌렀으니 터치음 설정은 무시. 무음·진동은 지킨다. 재생했으면 true
    fun preview(pack: SoundPack, effect: SfxEffect): Boolean {
        val ok = SoundPolicy.shouldPlay(SoundMode.ON, audio.ringer, audio.touchSoundsOn)
        if (ok) engine.play(pack.of(effect))
        return ok
    }
}

class AndroidDeviceAudio(context: Context) : DeviceAudio {
    private val app = context.applicationContext
    private val manager = app.getSystemService(AudioManager::class.java)

    override val ringer: RingerState
        get() = when (manager?.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> RingerState.NORMAL
            AudioManager.RINGER_MODE_VIBRATE -> RingerState.VIBRATE
            else -> RingerState.SILENT
        }

    override val touchSoundsOn: Boolean
        get() = Settings.System.getInt(
            app.contentResolver,
            Settings.System.SOUND_EFFECTS_ENABLED,
            1,
        ) !=
            0
}

// 처음 재생할 때 불러온다. 불러오기가 끝나면 바로 재생
class SoundPoolEngine(context: Context) : SfxEngine {
    private val app = context.applicationContext
    private val loaded = mutableMapOf<Int, Int>()
    private val pending = mutableSetOf<Int>()
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
        .apply {
            setOnLoadCompleteListener { pool, sampleId, status ->
                if (status == 0 && pending.remove(sampleId)) pool.play(sampleId, 1f, 1f, 1, 0, 1f)
            }
        }

    override fun play(res: Int) {
        val id = loaded[res]
        if (id != null) {
            pool.play(id, 1f, 1f, 1, 0, 1f)
        } else {
            val newId = pool.load(app, res, 1)
            loaded[res] = newId
            pending += newId
        }
    }

    private companion object {
        const val MAX_STREAMS = 2
    }
}

// 앱에서 하나 넣는다. 없으면 소리 없음
val LocalSfxPlayer = staticCompositionLocalOf<SfxPlayer?> { null }

// 효과음 + 햅틱. 설정(효과음·햅틱 끄기)을 지킨다
@Stable
class YhFeedback internal constructor(
    private val player: SfxPlayer?,
    private val pack: SoundPack,
    private val mode: SoundMode,
    private val hapticOn: Boolean,
    private val haptic: HapticFeedback,
) {
    fun sound(effect: SfxEffect) {
        player?.play(pack, effect, mode)
    }

    fun haptic(type: HapticFeedbackType) {
        if (hapticOn) haptic.performHapticFeedback(type)
    }
}

@Composable
fun rememberYhFeedback(): YhFeedback {
    val player = LocalSfxPlayer.current
    val state = YhTheme.state
    val haptic = LocalHapticFeedback.current
    val pack = state.spec.soundPack
    val mode = state.appearance.sound
    val hapticOn = state.appearance.haptic
    return remember(player, pack, mode, hapticOn, haptic) {
        YhFeedback(player, pack, mode, hapticOn, haptic)
    }
}
