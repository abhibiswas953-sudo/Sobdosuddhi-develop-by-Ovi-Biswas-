package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val NotoSerifBengaliFamily = FontFamily(
    Font(R.font.noto_serif_bengali, FontWeight.Normal),
    Font(R.font.noto_serif_bengali, FontWeight.Medium),
    Font(R.font.noto_serif_bengali, FontWeight.SemiBold),
    Font(R.font.noto_serif_bengali, FontWeight.Bold)
)

val NotoSansBengaliFamily = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
    Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

val PlayfairDisplayFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Medium),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

fun buildSobdosuddhiTypography(
    useSerifForBody: Boolean = true,
    scale: Float = 1.0f
): Typography {
    val bodyFamily = if (useSerifForBody) NotoSerifBengaliFamily else NotoSansBengaliFamily
    val displayFamily = NotoSerifBengaliFamily
    val uiFamily = NotoSansBengaliFamily
    val s = scale.coerceIn(0.85f, 1.35f)

    return Typography(
        displayLarge = TextStyle(
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (36 * s).sp,
            lineHeight = (44 * s).sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (28 * s).sp,
            lineHeight = (38 * s).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * s).sp,
            lineHeight = (34 * s).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20 * s).sp,
            lineHeight = (30 * s).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18 * s).sp,
            lineHeight = (26 * s).sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (19 * s).sp,
            lineHeight = (28 * s).sp
        ),
        titleMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * s).sp,
            lineHeight = (24 * s).sp
        ),
        titleSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * s).sp,
            lineHeight = (28 * s).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * s).sp,
            lineHeight = (23 * s).sp
        ),
        bodySmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12 * s).sp,
            lineHeight = (18 * s).sp
        ),
        labelLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp
        ),
        labelMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * s).sp,
            lineHeight = (16 * s).sp
        ),
        labelSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * s).sp,
            lineHeight = (14 * s).sp
        )
    )
}
