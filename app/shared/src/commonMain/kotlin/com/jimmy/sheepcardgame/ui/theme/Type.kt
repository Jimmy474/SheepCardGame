package com.jimmy.sheepcardgame.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import org.jetbrains.compose.resources.Font
import sheepcardgame.app.shared.generated.resources.Res
import sheepcardgame.app.shared.generated.resources.`SourGummy_Italic_VariableFont_wdth,wght`
import sheepcardgame.app.shared.generated.resources.`SourGummy_VariableFont_wdth,wght`

@Composable
fun getSourGummyFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.`SourGummy_VariableFont_wdth,wght`, weight = FontWeight.Normal, style = FontStyle.Normal, FontVariation.Settings(FontWeight.ExtraBold, FontStyle.Normal)),
        Font(Res.font.`SourGummy_VariableFont_wdth,wght`, weight = FontWeight.Bold, style = FontStyle.Normal, FontVariation.Settings(FontWeight.Black, FontStyle.Normal)),
        Font(Res.font.`SourGummy_Italic_VariableFont_wdth,wght`, weight = FontWeight.Normal, style = FontStyle.Italic, FontVariation.Settings(FontWeight.ExtraBold, FontStyle.Italic)),
        Font(Res.font.`SourGummy_Italic_VariableFont_wdth,wght`, weight = FontWeight.Bold, style = FontStyle.Italic, FontVariation.Settings(FontWeight.Black, FontStyle.Italic))
    )
}

val baseline = Typography()

@Composable
fun cardGameTypography(): Typography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    displayMedium = baseline.displayMedium.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    displaySmall = baseline.displaySmall.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    titleLarge = baseline.titleLarge.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    titleMedium = baseline.titleMedium.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    titleSmall = baseline.titleSmall.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    bodySmall = baseline.bodySmall.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    labelLarge = baseline.labelLarge.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    labelMedium = baseline.labelMedium.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
    labelSmall = baseline.labelSmall.copy(fontFamily = getSourGummyFontFamily(), lineHeight = 1.em, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)),
)

