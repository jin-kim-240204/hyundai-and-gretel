package com.jinkim.hyundaiAndGretel.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jinkim.hyundaiAndGretel.ui.theme.PleosTheme
import com.jinkim.hyundaiAndGretel.ui.theme.Typography

// Components 67:22525. Primary is the light basic style; Secondary is the filled style.
enum class PleosButtonType { Primary, Secondary }

enum class PleosButtonSize(
    val minHeight: Dp,
    val minWidth: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val gap: Dp,
    val radius: Dp,
    val iconSize: Dp,
    val textStyle: TextStyle,
) {
    Large(100.dp, 140.dp, 40.dp, 16.dp, 16.dp, 32.dp, 52.dp, Typography.titleSmall),
    Medium(80.dp, 120.dp, 32.dp, 16.dp, 14.dp, 24.dp, 40.dp, Typography.bodyLarge),
    Small(64.dp, 100.dp, 24.dp, 8.dp, 12.dp, 16.dp, 32.dp, Typography.bodySmall),
}

@Composable
fun PleosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: PleosButtonType = PleosButtonType.Primary,
    size: PleosButtonSize = PleosButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = PleosTheme.colors
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val interactive = enabled && !loading
    val filled = type == PleosButtonType.Secondary
    val container = when {
        !interactive -> if (filled) colors.buttonFilledDisabled else colors.buttonBasicDisabled
        pressed -> if (filled) colors.buttonFilledPressed else colors.buttonBasicPressed
        else -> if (filled) colors.buttonFilled else colors.buttonBasic
    }
    val foreground = when {
        !interactive -> colors.buttonDisabledText
        filled -> colors.buttonFilledText
        else -> colors.buttonText
    }
    Surface(
        onClick = onClick,
        enabled = interactive,
        interactionSource = interactions,
        modifier = modifier.semantics { role = Role.Button }
            .widthIn(min = size.minWidth).heightIn(min = size.minHeight),
        shape = RoundedCornerShape(size.radius),
        color = container,
        contentColor = foreground,
        border = if (interactive) null else BorderStroke(2.dp, colors.buttonDisabledBorder),
    ) {
        Row(
            Modifier.padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(size.gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(size.iconSize), color = foreground, strokeWidth = 3.dp)
            } else if (leadingIcon != null) {
                Box(Modifier.size(size.iconSize), contentAlignment = Alignment.Center) { leadingIcon() }
            }
            Text(text, style = size.textStyle.copy(fontWeight = FontWeight.Bold))
            if (!loading && trailingIcon != null) {
                Box(Modifier.size(size.iconSize), contentAlignment = Alignment.Center) { trailingIcon() }
            }
        }
    }
}
