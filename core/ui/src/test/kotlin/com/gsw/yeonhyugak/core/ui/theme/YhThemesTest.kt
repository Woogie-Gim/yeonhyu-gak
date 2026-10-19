package com.gsw.yeonhyugak.core.ui.theme

import com.gsw.yeonhyugak.core.engine.appearance.CheerColor
import com.gsw.yeonhyugak.core.engine.appearance.ThemeId
import com.gsw.yeonhyugak.core.ui.MAX_PARTICLES
import com.gsw.yeonhyugak.core.ui.particlesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YhThemesTest {
    @Test
    fun everyThemeId_hasSpec() {
        assertEquals(ThemeId.entries.toSet(), YhThemes.all.map { it.id }.toSet())
        ThemeId.entries.forEach { assertEquals(it, YhThemes.of(it).id) }
    }

    @Test
    fun lightAndDark_differ() = YhThemes.all.forEach {
        assertNotEquals("${it.id}", it.light, it.dark)
    }

    @Test
    fun season_changesAccentByMonth() {
        val accents = (1..12).map { YhThemes.Season.colors(dark = false, month = it).leaveFill }
        assertTrue(accents.toSet().size >= 6)
    }

    @Test
    fun otherThemes_ignoreMonth() = YhThemes.all.filter { it.id != ThemeId.SEASON }.forEach {
        assertEquals("${it.id}", it.colors(false, month = 1), it.colors(false, month = 7))
    }

    // 응원 색은 장식 줄에만. 연차 채움·글자는 그대로
    @Test
    fun cheer_changesDecorOnly() {
        val red = YhThemes.Baseball.colors(false, cheer = CheerColor.RED)
        val navy = YhThemes.Baseball.colors(false, cheer = CheerColor.NAVY)
        assertNotEquals(red.decor, navy.decor)
        assertEquals(red.copy(decor = navy.decor), navy)
    }

    @Test
    fun soundPacks_unique() {
        val ids = YhThemes.all.flatMap { s -> SfxEffect.entries.map { s.soundPack.of(it) } }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun particles_atMost20_andStablePerMonth() {
        (1..12).forEach { m ->
            assertTrue(particlesOf(m).size <= MAX_PARTICLES)
            assertEquals(particlesOf(m), particlesOf(m))
        }
        assertTrue(particlesOf(1, count = 100).size <= MAX_PARTICLES)
    }
}
