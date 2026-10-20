package com.gsw.yeonhyugak.feature.share

import com.gsw.yeonhyugak.core.engine.holiday.Holiday
import java.time.LocalDate

// 공유 카드에 들어가는 값 전부. 메모·입사일 같은 개인 정보 필드는 두지 않는다
data class ShareCardData(
    val year: Int,
    val blocks: List<ShareBlock>,
    val totalDays: Int,
    val usedLeave: Double,
    // 미발표 연도면 예상 배지
    val expected: Boolean,
    val showQr: Boolean,
)

// 연휴 한 줄. holiday = 이름 표시용 공휴일(없으면 null)
data class ShareBlock(
    val start: LocalDate,
    val end: LocalDate,
    val days: Int,
    val holiday: Holiday?,
)

// maxRows = 카드에 올리는 연휴 줄 수(줄 50dp + `외 n개` 줄이 들어가는 만큼). 넘치면 `외 n개 연휴`
enum class ShareRatio(val width: Int, val height: Int, val maxRows: Int) {
    FEED(FEED_W, FEED_H, FEED_ROWS),
    STORY(STORY_W, STORY_H, STORY_ROWS),
}

private const val FEED_W = 4
private const val FEED_H = 5
private const val FEED_ROWS = 3
private const val STORY_W = 9
private const val STORY_H = 16
private const val STORY_ROWS = 6

// QR에 넣는 주소. debug 접미사 없이 스토어 패키지명 고정
const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.gsw.yeonhyugak"
