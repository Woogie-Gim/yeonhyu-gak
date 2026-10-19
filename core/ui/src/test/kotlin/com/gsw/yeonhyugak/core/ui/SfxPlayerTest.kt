package com.gsw.yeonhyugak.core.ui

import com.gsw.yeonhyugak.core.engine.appearance.RingerState
import com.gsw.yeonhyugak.core.engine.appearance.SoundMode
import com.gsw.yeonhyugak.core.ui.theme.SfxEffect
import com.gsw.yeonhyugak.core.ui.theme.YhThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// 무음·진동 모드에서 재생 호출 0회
class SfxPlayerTest {
    private class FakeEngine : SfxEngine {
        val played = mutableListOf<Int>()

        override fun play(res: Int) {
            played += res
        }
    }

    private class FakeAudio(
        override val ringer: RingerState,
        override val touchSoundsOn: Boolean = true,
    ) : DeviceAudio

    private val pack = YhThemes.Note.soundPack

    @Test
    fun silentOrVibrate_neverPlays() {
        listOf(RingerState.SILENT, RingerState.VIBRATE).forEach { ringer ->
            val engine = FakeEngine()
            val player = SfxPlayer(engine, FakeAudio(ringer))
            SoundMode.entries.forEach { mode ->
                SfxEffect.entries.forEach { player.play(pack, it, mode) }
            }
            assertFalse(player.preview(pack, SfxEffect.MARK))
            assertEquals("$ringer", 0, engine.played.size)
        }
    }

    @Test
    fun normal_playsPerSetting() {
        val engine = FakeEngine()
        val player = SfxPlayer(engine, FakeAudio(RingerState.NORMAL, touchSoundsOn = false))
        player.play(pack, SfxEffect.MARK, SoundMode.OFF)
        player.play(pack, SfxEffect.MARK, SoundMode.SYSTEM)
        player.play(pack, SfxEffect.PAGE, SoundMode.ON)
        assertEquals(listOf(pack.page), engine.played)
    }
}
