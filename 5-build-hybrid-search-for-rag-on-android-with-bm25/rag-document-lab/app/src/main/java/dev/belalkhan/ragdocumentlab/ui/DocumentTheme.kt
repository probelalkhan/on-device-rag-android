package dev.belalkhan.ragdocumentlab.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import dev.belalkhan.ragdocumentlab.R

@OptIn(ExperimentalTextApi::class)
private val manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
    headlineSmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = manrope, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 18.sp)
)

private val shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun DocumentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF4355C5),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE2E6FF),
            onPrimaryContainer = Color(0xFF18245F),
            secondary = Color(0xFF006B5D),
            secondaryContainer = Color(0xFF9CF2DD),
            background = Color(0xFFF7F7FA),
            surface = Color.White,
            surfaceContainer = Color(0xFFF0F0F5),
            onSurface = Color(0xFF1B1B20),
            onBackground = Color(0xFF1B1B20),
            onSurfaceVariant = Color(0xFF62636C),
            outline = Color(0xFFAAABB4),
            outlineVariant = Color(0xFFE1E1E8)
        ),
        typography = typography,
        shapes = shapes,
        content = content
    )
}
