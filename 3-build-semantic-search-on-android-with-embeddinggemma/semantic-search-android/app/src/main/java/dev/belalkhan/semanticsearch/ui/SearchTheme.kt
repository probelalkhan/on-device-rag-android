package dev.belalkhan.semanticsearch.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.belalkhan.semanticsearch.R

@OptIn(ExperimentalTextApi::class)
private val manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)
private val typography = Typography(
    headlineLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
    titleLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 18.sp)
)

@Composable
fun SearchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF176B59),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0EEE6),
            onPrimaryContainer = Color(0xFF174C40),
            background = Color(0xFFF5F5F0),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF182B25),
            onBackground = Color(0xFF182B25),
            onSurfaceVariant = Color(0xFF67736D),
            outline = Color(0xFFB5C1B9),
            outlineVariant = Color(0xFFE1E6DF)
        ),
        typography = typography,
        content = content
    )
}
