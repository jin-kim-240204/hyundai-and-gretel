package com.jinkim.hyundaiAndGretel.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Design 39675:4528 / 4537. App semantic colors; Foundation's full palette is pending.
@Immutable
data class PleosColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val buttonBasic: Color,
    val buttonBasicPressed: Color,
    val buttonBasicDisabled: Color,
    val buttonFilled: Color,
    val buttonFilledPressed: Color,
    val buttonFilledDisabled: Color,
    val buttonText: Color,
    val buttonFilledText: Color,
    val buttonDisabledText: Color,
    val buttonDisabledBorder: Color,
    val isDark: Boolean,
)

internal val PleosLightColors = PleosColors(
    background = Color(0xFFF7F8FA), surface = Color.White,
    textPrimary = Color(0xFF131417), textSecondary = Color(0xFF555863),
    divider = Color(0xFFD5D9E2),
    buttonBasic = Color(0x0D131417), buttonBasicPressed = Color(0x1A131417),
    buttonBasicDisabled = Color(0x33FFFFFF),
    buttonFilled = Color(0xFF44464E), buttonFilledPressed = Color(0xFF313236),
    buttonFilledDisabled = Color(0x0D131417),
    buttonText = Color(0xFF131417), buttonFilledText = Color.White,
    buttonDisabledText = Color(0x4D131417), buttonDisabledBorder = Color(0x1A131417),
    isDark = false,
)

// shortcut: night button variants are unread; verify against their Figma table when access resumes.
internal val PleosDarkColors = PleosLightColors.copy(
    background = Color(0xFF1D1E21), surface = Color(0xFF313236),
    textPrimary = Color.White, textSecondary = Color(0xFFBAC0CA),
    divider = Color(0xFF3C3E45),
    buttonBasic = Color(0xFF44464E), buttonBasicPressed = Color(0xFF555863),
    buttonBasicDisabled = Color(0xFF313236), buttonFilledDisabled = Color(0xFF313236),
    buttonText = Color.White, buttonDisabledText = Color(0x4DFFFFFF),
    buttonDisabledBorder = Color(0x1AFFFFFF), isDark = true,
)
