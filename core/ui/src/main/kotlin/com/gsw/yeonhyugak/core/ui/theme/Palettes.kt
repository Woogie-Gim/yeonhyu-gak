package com.gsw.yeonhyugak.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.gsw.yeonhyugak.core.engine.appearance.CheerColor

// 테마별 색 값 (SPEC 7장 표 기준, 대비 테스트로 보정)

// 기본 노트
internal val NoteLightColors = YhColors(
    background = Color(0xFFFFFFFF),
    ink = Color(0xFF1E2430),
    muted = Color(0xFF5B6475),
    outline = Color(0xFFD5DAE3),
    holiday = Color(0xFFD7263D),
    saturday = Color(0xFF2E5AAC),
    // SPEC #3A86FF는 흰 글자 3.5:1이라 진하게
    accent = Color(0xFF2563EB),
    onAccent = Color(0xFFFFFFFF),
    warning = Color(0xFFB54708),
    onWarning = Color(0xFFFFFFFF),
    bandFill = Color(0xFF2E6BD6),
    bandLabel = Color(0xFF1E2430),
    leaveFill = Color(0xFF2563EB),
    onLeaveFill = Color(0xFFFFFFFF),
    halfLeaveFill = Color(0xFFF0F5FE),
    todayRing = Color(0xFF1E2430),
    todayRingOnLeave = Color(0xFFFFFFFF),
    todayText = Color(0xFF1E2430),
    snackbarContainer = Color(0xFF1E2430),
    snackbarText = Color(0xFFFFFFFF),
    snackbarAction = Color(0xFF8AB4FF),
    memoDot = Color(0xFF5B6475),
    backdropInk = Color(0xFFF3F5F8),
)

internal val NoteDarkColors = YhColors(
    background = Color(0xFF14181F),
    ink = Color(0xFFE6E9EF),
    muted = Color(0xFFA3ACBB),
    outline = Color(0xFF3A4250),
    holiday = Color(0xFFFF7A85),
    saturday = Color(0xFF8AB4F8),
    accent = Color(0xFF8AB4FF),
    onAccent = Color(0xFF0B1220),
    warning = Color(0xFFFFB366),
    onWarning = Color(0xFF1F1300),
    bandFill = Color(0xFF5B8FE0),
    bandLabel = Color(0xFFE6E9EF),
    leaveFill = Color(0xFF6EA8FF),
    onLeaveFill = Color(0xFF0B1220),
    halfLeaveFill = Color(0xFF22324D),
    todayRing = Color(0xFFE6E9EF),
    todayRingOnLeave = Color(0xFF0B1220),
    todayText = Color(0xFFE6E9EF),
    snackbarContainer = Color(0xFFE6E9EF),
    snackbarText = Color(0xFF14181F),
    snackbarAction = Color(0xFF1D4ED8),
    memoDot = Color(0xFFA3ACBB),
    backdropInk = Color(0xFF1B2029),
)

// 계절: 바탕은 고정, 강조색만 달마다 바뀐다
private val SeasonLightBase = NoteLightColors.copy(background = Color(0xFFFCFCFA))
private val SeasonDarkBase = NoteDarkColors.copy(
    background = Color(0xFF121619),
    outline = Color(0xFF363E4A),
    onAccent = Color(0xFF0E1116),
    onLeaveFill = Color(0xFF0E1116),
    todayRingOnLeave = Color(0xFF0E1116),
    snackbarText = Color(0xFF121619),
)

// SPEC 월별 강조색(1월 #7FA7D9 · 4월 #F2A7B8 · 7월 #2BB3C0 · 10월 #D9822B · 12월 #3D5A80)을
// 라이트는 진하게, 다크는 밝게 맞춘 값
// accent · leaveFill · bandFill · halfLeaveFill · backdropInk · snackbarAction
private val SeasonLightMonths = listOf(
    MonthTint(0xFF597598, 0xFF5A779A, 0xFF6D90BB, 0xFFF2F6F9, 0xFFF2F6F7, 0xFF7FA7D9),
    MonthTint(0xFF65748B, 0xFF66768D, 0xFF7C8FAB, 0xFFF3F6F9, 0xFFF4F6F7, 0xFF9BB3D6),
    MonthTint(0xFF946673, 0xFF976875, 0xFFB77E8E, 0xFFFBF4F5, 0xFFFAF4F3, 0xFFE8A0B4),
    MonthTint(0xFF946670, 0xFF966872, 0xFFB67D8A, 0xFFFCF3F4, 0xFFFBF4F4, 0xFFF2A7B8),
    MonthTint(0xFF517D5A, 0xFF527F5B, 0xFF639A6E, 0xFFF0F7F0, 0xFFF0F7F0, 0xFF7CC08A),
    MonthTint(0xFF387F6D, 0xFF39816F, 0xFF459C86, 0xFFEFF7F4, 0xFFEFF7F3, 0xFF4FB39A),
    MonthTint(0xFF1E7D86, 0xFF1F7F88, 0xFF259AA5, 0xFFECF8F8, 0xFFEEF7F6, 0xFF2BB3C0),
    MonthTint(0xFF217C91, 0xFF227F95, 0xFF299AB4, 0xFFEEF7F8, 0xFFF0F7F6, 0xFF2A9DB8),
    MonthTint(0xFF916D27, 0xFF946E27, 0xFFB28530, 0xFFFAF5E9, 0xFFF9F5EA, 0xFFD9A23A),
    MonthTint(0xFFA56321, 0xFFA76421, 0xFFCC7A28, 0xFFFBF4ED, 0xFFFAF5ED, 0xFFD9822B),
    MonthTint(0xFFA76037, 0xFFA96237, 0xFFB86A3C, 0xFFFAF5F1, 0xFFF8F5F0, 0xFFC17C53),
    MonthTint(0xFF3D5A80, 0xFF3D5A80, 0xFF3D5A80, 0xFFF4F6F7, 0xFFF4F6F5, 0xFF7B8FA9),
)

private val SeasonDarkMonths = listOf(
    MonthTint(0xFF7FA7D9, 0xFF7FA7D9, 0xFF7FA7D9, 0xFF2A3643, 0xFF222C36, 0xFF506989),
    MonthTint(0xFF9BB3D6, 0xFF9BB3D6, 0xFF9BB3D6, 0xFF303943, 0xFF272E35, 0xFF5A687C),
    MonthTint(0xFFE8A0B4, 0xFFE8A0B4, 0xFFE8A0B4, 0xFF41343B, 0xFF322B30, 0xFF845B67),
    MonthTint(0xFFF2A7B8, 0xFFF2A7B8, 0xFFF2A7B8, 0xFF43363C, 0xFF342C31, 0xFF855C65),
    MonthTint(0xFF7CC08A, 0xFF7CC08A, 0xFF7CC08A, 0xFF293B32, 0xFF22302A, 0xFF486F50),
    MonthTint(0xFF4FB39A, 0xFF4FB39A, 0xFF4FB39A, 0xFF1F3935, 0xFF1B2E2C, 0xFF327161),
    MonthTint(0xFF2BB3C0, 0xFF2BB3C0, 0xFF2BB3C0, 0xFF18393E, 0xFF162E32, 0xFF1B7179),
    MonthTint(0xFF2A9DB8, 0xFF2A9DB8, 0xFF2A9DB8, 0xFF17343C, 0xFF162A31, 0xFF1E7184),
    MonthTint(0xFFD9A23A, 0xFFD9A23A, 0xFFD9A23A, 0xFF3E3520, 0xFF302B1E, 0xFF826123),
    MonthTint(0xFFD9822B, 0xFFD9822B, 0xFFD9822B, 0xFF3E2E1D, 0xFF30261C, 0xFF94581D),
    MonthTint(0xFFB96D40, 0xFFB96D40, 0xFFB86A3C, 0xFF372821, 0xFF2B231E, 0xFF975731),
    MonthTint(0xFF6D839F, 0xFF6D839F, 0xFF546E8F, 0xFF1B2530, 0xFF182028, 0xFF3D5A80),
)

internal fun seasonColors(month: Int, dark: Boolean): YhColors {
    val i = (month - 1).coerceIn(0, MONTHS - 1)
    return if (dark) {
        SeasonDarkBase.tinted(
            SeasonDarkMonths[i],
        )
    } else {
        SeasonLightBase.tinted(SeasonLightMonths[i])
    }
}

// 아날로그: 모눈 다이어리
// SPEC 형광펜 #F5E663은 크림 바탕 대비 1.2:1이라 라이트는 진한 주황 형광펜으로 채움
internal val AnalogLightColors = YhColors(
    background = Color(0xFFF6F4EC),
    ink = Color(0xFF23395B),
    muted = Color(0xFF55617A),
    outline = Color(0xFFB9CCE4),
    holiday = Color(0xFFB5332E),
    saturday = Color(0xFF2F5D9A),
    accent = Color(0xFF2F5D9A),
    onAccent = Color(0xFFFFFFFF),
    warning = Color(0xFF9A3F06),
    onWarning = Color(0xFFFFFFFF),
    bandFill = Color(0xFF3E6FB0),
    bandLabel = Color(0xFF23395B),
    leaveFill = Color(0xFFB8530F),
    onLeaveFill = Color(0xFFFFFFFF),
    halfLeaveFill = Color(0xFFFBEBD9),
    todayRing = Color(0xFF23395B),
    todayRingOnLeave = Color(0xFFFFFFFF),
    todayText = Color(0xFF23395B),
    snackbarContainer = Color(0xFF23395B),
    snackbarText = Color(0xFFF6F4EC),
    snackbarAction = Color(0xFFF5E663),
    memoDot = Color(0xFF55617A),
    // 모눈선
    backdropInk = Color(0xFFE3E9F1),
)

// 다크는 SPEC 형광펜 노랑을 그대로 쓴다
internal val AnalogDarkColors = YhColors(
    background = Color(0xFF1C1F26),
    ink = Color(0xFFEDE8D8),
    muted = Color(0xFFB3B0A3),
    outline = Color(0xFF3C4558),
    holiday = Color(0xFFFF8A80),
    saturday = Color(0xFF9DBDF2),
    accent = Color(0xFF9DBDF2),
    onAccent = Color(0xFF10141C),
    warning = Color(0xFFFFB366),
    onWarning = Color(0xFF1F1300),
    bandFill = Color(0xFF7EA3DA),
    bandLabel = Color(0xFFEDE8D8),
    leaveFill = Color(0xFFF5E663),
    onLeaveFill = Color(0xFF1C1F26),
    halfLeaveFill = Color(0xFF3A3720),
    todayRing = Color(0xFFEDE8D8),
    todayRingOnLeave = Color(0xFF1C1F26),
    todayText = Color(0xFFEDE8D8),
    snackbarContainer = Color(0xFFEDE8D8),
    snackbarText = Color(0xFF1C1F26),
    snackbarAction = Color(0xFF7A5A00),
    memoDot = Color(0xFFB3B0A3),
    backdropInk = Color(0xFF262B35),
)

// Lo-fi: 라이트 = SPEC 라이트 변형(#DCD6E8), 다크 = SPEC 기본(#2F3A4A)
internal val LofiLightColors = YhColors(
    background = Color(0xFFDCD6E8),
    ink = Color(0xFF2A2438),
    muted = Color(0xFF4F4766),
    outline = Color(0xFFB4ABCB),
    holiday = Color(0xFFA12B3A),
    saturday = Color(0xFF2F5288),
    accent = Color(0xFF7A3E0C),
    onAccent = Color(0xFFFFFFFF),
    warning = Color(0xFF7A3E0C),
    onWarning = Color(0xFFFFFFFF),
    bandFill = Color(0xFF5A4A8A),
    bandLabel = Color(0xFF2A2438),
    leaveFill = Color(0xFF8A4A14),
    onLeaveFill = Color(0xFFFFFFFF),
    halfLeaveFill = Color(0xFFEEDCC8),
    todayRing = Color(0xFF2A2438),
    todayRingOnLeave = Color(0xFFFFFFFF),
    todayText = Color(0xFF2A2438),
    snackbarContainer = Color(0xFF2F3A4A),
    snackbarText = Color(0xFFEADFC8),
    snackbarAction = Color(0xFFF2A65A),
    memoDot = Color(0xFF4F4766),
    backdropInk = Color(0xFFD2CCE2),
)

internal val LofiDarkColors = YhColors(
    background = Color(0xFF2F3A4A),
    ink = Color(0xFFEADFC8),
    muted = Color(0xFFC2B9A6),
    outline = Color(0xFF4A5668),
    // SPEC #E86A6A는 바탕 대비 3.7:1이라 밝게
    holiday = Color(0xFFF59A9A),
    saturday = Color(0xFFA9CBEA),
    accent = Color(0xFFF2A65A),
    onAccent = Color(0xFF2A1A08),
    warning = Color(0xFFFFC48A),
    onWarning = Color(0xFF2A1A08),
    bandFill = Color(0xFFC9A2E0),
    bandLabel = Color(0xFFEADFC8),
    leaveFill = Color(0xFFF2A65A),
    onLeaveFill = Color(0xFF2A1A08),
    halfLeaveFill = Color(0xFF4A4440),
    todayRing = Color(0xFFEADFC8),
    todayRingOnLeave = Color(0xFF2A1A08),
    todayText = Color(0xFFEADFC8),
    snackbarContainer = Color(0xFFEADFC8),
    snackbarText = Color(0xFF2F3A4A),
    snackbarAction = Color(0xFF8A4A14),
    memoDot = Color(0xFFC2B9A6),
    backdropInk = Color(0xFF364254),
)

// 야구: 두 모드 모두 어두운 전광판. 연차 = 전구 노랑
internal val BaseballLightColors = YhColors(
    background = Color(0xFF0E3B2E),
    ink = Color(0xFFF7F3E3),
    muted = Color(0xFFC9D6C8),
    outline = Color(0xFF2E5E4E),
    // SPEC #FF6B5B는 바탕 대비 4.46:1이라 밝게
    holiday = Color(0xFFFF8B7E),
    saturday = Color(0xFF9AD6FA),
    accent = Color(0xFFF2C14E),
    onAccent = Color(0xFF1A1405),
    warning = Color(0xFFFFB366),
    onWarning = Color(0xFF1F1300),
    bandFill = Color(0xFFF7F3E3),
    bandLabel = Color(0xFFF7F3E3),
    leaveFill = Color(0xFFF2C14E),
    onLeaveFill = Color(0xFF1A1405),
    halfLeaveFill = Color(0xFF1B472F),
    todayRing = Color(0xFFF7F3E3),
    todayRingOnLeave = Color(0xFF1A1405),
    todayText = Color(0xFFF7F3E3),
    snackbarContainer = Color(0xFFF7F3E3),
    snackbarText = Color(0xFF0E3B2E),
    snackbarAction = Color(0xFF8A5A00),
    memoDot = Color(0xFFC9D6C8),
    backdropInk = Color(0xFF134536),
)

internal val BaseballDarkColors = YhColors(
    background = Color(0xFF0A1712),
    ink = Color(0xFFEDEBDF),
    muted = Color(0xFFB4BFB6),
    outline = Color(0xFF22382E),
    holiday = Color(0xFFFF8B7E),
    saturday = Color(0xFF9AD6FA),
    accent = Color(0xFFE8B83F),
    onAccent = Color(0xFF1A1405),
    warning = Color(0xFFFFB366),
    onWarning = Color(0xFF1F1300),
    bandFill = Color(0xFFEDEBDF),
    bandLabel = Color(0xFFEDEBDF),
    leaveFill = Color(0xFFE8B83F),
    onLeaveFill = Color(0xFF1A1405),
    halfLeaveFill = Color(0xFF1D2A1A),
    todayRing = Color(0xFFEDEBDF),
    todayRingOnLeave = Color(0xFF1A1405),
    todayText = Color(0xFFEDEBDF),
    snackbarContainer = Color(0xFFEDEBDF),
    snackbarText = Color(0xFF0A1712),
    snackbarAction = Color(0xFF7A5000),
    memoDot = Color(0xFFB4BFB6),
    backdropInk = Color(0xFF11221B),
)

// 응원 색: 전광판 테두리 줄에만 쓰는 장식. 글자·연차 표시에는 쓰지 않는다
fun cheerSwatch(cheer: CheerColor): Color = when (cheer) {
    CheerColor.RED -> Color(0xFFD93A3A)
    CheerColor.BLUE -> Color(0xFF2F6FD6)
    CheerColor.NAVY -> Color(0xFF26386B)
    CheerColor.ORANGE -> Color(0xFFF07C1E)
    CheerColor.GREEN -> Color(0xFF2E9E5B)
    CheerColor.BLACK -> Color(0xFF111111)
}

internal fun baseballColors(cheer: CheerColor, dark: Boolean): YhColors =
    (if (dark) BaseballDarkColors else BaseballLightColors).copy(decor = cheerSwatch(cheer))

private const val MONTHS = 12
