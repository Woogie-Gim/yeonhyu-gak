package com.gsw.yeonhyugak.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.gsw.yeonhyugak.core.ui.R

// 번들 폰트는 서브셋 (tools/fonts · licenses/README.md)
// 본문: Pretendard 서브셋
internal val BodyFont = FontFamily(
    Font(R.font.yh_body_regular, FontWeight.Normal),
    Font(R.font.yh_body_medium, FontWeight.Medium),
    Font(R.font.yh_body_bold, FontWeight.Bold),
)

// 아날로그 숫자·제목: 나눔손글씨 펜
internal val PenFont = FontFamily(Font(R.font.yh_pen, FontWeight.Normal))

// Lo-fi 제목: Galmuri11
internal val PixelFont = FontFamily(
    Font(R.font.yh_pixel, FontWeight.Normal),
    Font(R.font.yh_pixel_bold, FontWeight.Bold),
)

// 야구 숫자: Doto 점 매트릭스 (숫자만 있음)
internal val DotFont = FontFamily(Font(R.font.yh_dot, FontWeight.Normal))

// 숫자·제목만 테마 폰트. 본문은 모든 테마에서 Pretendard
@Immutable
data class YhFonts(val numeral: FontFamily, val title: FontFamily, val body: FontFamily = BodyFont)

// 글자 중 숫자만 숫자 폰트로. 숫자 전용 폰트(야구)에서 한글이 시스템 폰트로 빠지지 않게
fun YhFonts.numeralText(text: String): AnnotatedString = buildAnnotatedString {
    text.forEach { c ->
        if (c.isDigit()) withStyle(SpanStyle(fontFamily = numeral)) { append(c) } else append(c)
    }
}

internal fun typographyOf(fonts: YhFonts): Typography {
    val base = Typography(
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
    )
    val title: TextStyle.() -> TextStyle = { copy(fontFamily = fonts.title) }
    val body: TextStyle.() -> TextStyle = { copy(fontFamily = fonts.body) }
    return base.copy(
        displayLarge = base.displayLarge.title(),
        displayMedium = base.displayMedium.title(),
        displaySmall = base.displaySmall.title(),
        headlineLarge = base.headlineLarge.title(),
        headlineMedium = base.headlineMedium.title(),
        headlineSmall = base.headlineSmall.title(),
        titleLarge = base.titleLarge.title(),
        titleMedium = base.titleMedium.body(),
        titleSmall = base.titleSmall.body(),
        bodyLarge = base.bodyLarge.body(),
        bodyMedium = base.bodyMedium.body(),
        bodySmall = base.bodySmall.body(),
        labelLarge = base.labelLarge.body(),
        labelMedium = base.labelMedium.body(),
        labelSmall = base.labelSmall.body(),
    )
}
