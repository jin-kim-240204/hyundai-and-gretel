package com.jinkim.hyundaiAndGretel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jinkim.hyundaiAndGretel.R

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val AstaSans = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
        .map { weight ->
            Font(R.font.asta_sans, weight = weight,
                variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
        },
)

private fun textStyle(size: Int, height: Int, weight: FontWeight) = TextStyle(
    fontFamily = AstaSans, fontWeight = weight,
    fontSize = size.sp, lineHeight = height.sp, letterSpacing = 0.sp,
)

// Foundation 67:12330 / 12341 / 12352 / 12363; alternate weights use copy(fontWeight = ...).
val Typography = Typography(
    headlineLarge = textStyle(60, 80, FontWeight.ExtraBold),
    headlineMedium = textStyle(56, 72, FontWeight.ExtraBold),
    headlineSmall = textStyle(48, 64, FontWeight.ExtraBold),
    titleLarge = textStyle(40, 52, FontWeight.Bold),
    titleMedium = textStyle(36, 44, FontWeight.Bold),
    titleSmall = textStyle(32, 40, FontWeight.Bold),
    bodyLarge = textStyle(30, 38, FontWeight.Normal),
    bodyMedium = textStyle(28, 36, FontWeight.Normal),
    bodySmall = textStyle(26, 34, FontWeight.Normal),
    labelLarge = textStyle(24, 28, FontWeight.Normal),
    labelMedium = textStyle(24, 28, FontWeight.Normal),
    labelSmall = textStyle(20, 24, FontWeight.Normal),
)

// Design screen typography differs from the Foundation component samples.
object RoadNoteTypography {
    val header = TextStyle(fontFamily = AstaSans, fontSize = 29.sp, fontWeight = FontWeight.Bold)
    val cardTitle = TextStyle(fontFamily = AstaSans, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    val supporting = TextStyle(fontFamily = AstaSans, fontSize = 16.sp)
    val messageLabel = TextStyle(fontFamily = AstaSans, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    val message = TextStyle(fontFamily = AstaSans, fontSize = 38.sp, fontWeight = FontWeight.Bold)
    val compactMessage = message.copy(fontSize = 31.sp)
    val status = TextStyle(fontFamily = AstaSans, fontSize = 19.sp, fontWeight = FontWeight.Medium)
    val action = TextStyle(fontFamily = AstaSans, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}
