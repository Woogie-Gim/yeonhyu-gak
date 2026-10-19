package com.gsw.yeonhyugak.core.engine.appearance

// 테마 종류 (SPEC 7장). 저장은 name으로
enum class ThemeId { NOTE, SEASON, ANALOG, LOFI, BASEBALL }

// 야구 테마 응원 색. 일반 색 이름만 쓴다 (구단 연상 이름 금지)
enum class CheerColor { RED, BLUE, NAVY, ORANGE, GREEN, BLACK }

// 효과음 설정. SYSTEM = 기기 터치음 설정을 따름
enum class SoundMode { SYSTEM, ON, OFF }

// 기기 벨소리 모드
enum class RingerState { NORMAL, VIBRATE, SILENT }

data class Appearance(
    val theme: ThemeId = ThemeId.NOTE,
    val cheer: CheerColor = CheerColor.RED,
    val sound: SoundMode = SoundMode.SYSTEM,
    val haptic: Boolean = true,
)

object SoundPolicy {
    // 무음·진동이면 어떤 설정이든 재생 안 함 (CLAUDE.md 원칙 8)
    fun shouldPlay(mode: SoundMode, ringer: RingerState, touchSoundsOn: Boolean): Boolean = when {
        ringer != RingerState.NORMAL -> false
        mode == SoundMode.OFF -> false
        mode == SoundMode.SYSTEM -> touchSoundsOn
        else -> true
    }
}

object BaseballSeason {
    private val OFF_SEASON = setOf(11, 12, 1, 2)

    // 비시즌(11~2월)은 조명 꺼진 전광판
    fun isOn(month: Int): Boolean = month !in OFF_SEASON
}
