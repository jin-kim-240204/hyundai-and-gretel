package com.jinkim.hyundaiAndGretel.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.ui.theme.PleosSpacing
import com.jinkim.hyundaiAndGretel.ui.theme.PleosTheme
import com.jinkim.hyundaiAndGretel.ui.theme.RoadNoteDimensions as Dimensions
import com.jinkim.hyundaiAndGretel.ui.theme.RoadNoteTypography as Type

enum class RoadNoteTone { Warning, Information }

@Composable
fun RoadNoteHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = PleosTheme.colors.textPrimary, style = Type.header,
            modifier = Modifier.padding(top = Dimensions.headerTop, bottom = Dimensions.headerBottom))
        HorizontalDivider(color = PleosTheme.colors.divider)
    }
}

@Composable
fun RoadNoteStateCard(title: String, description: String, modifier: Modifier = Modifier) {
    Surface(modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(Dimensions.statusCardRadius), color = PleosTheme.colors.surface) {
        Column(Modifier.fillMaxWidth().padding(
            horizontal = Dimensions.statusCardHorizontalPadding,
            vertical = Dimensions.statusCardVerticalPadding,
        ), verticalArrangement = Arrangement.spacedBy(PleosSpacing.space05)) {
            Text(title, style = Type.cardTitle, color = PleosTheme.colors.textPrimary)
            Text(description, style = Type.supporting, color = PleosTheme.colors.textSecondary)
        }
    }
}

@Composable
fun RoadNoteBadge(tone: RoadNoteTone, modifier: Modifier = Modifier) {
    val resource = when (tone) {
        RoadNoteTone.Warning -> if (PleosTheme.colors.isDark) R.drawable.note_warning_dark else R.drawable.note_warning_light
        RoadNoteTone.Information -> R.drawable.note_information
    }
    Image(painterResource(resource), contentDescription = null, modifier = modifier.size(Dimensions.icon))
}

@Composable
fun RoadNoteMessageCard(
    label: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: RoadNoteTone = RoadNoteTone.Warning,
    compact: Boolean = false,
) {
    Surface(modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(Dimensions.cardRadius), color = PleosTheme.colors.surface) {
        Column(Modifier.fillMaxWidth().padding(
            start = Dimensions.cardPadding, end = Dimensions.cardPadding, top = Dimensions.cardPadding,
            bottom = if (compact) Dimensions.compactMessageBottom else Dimensions.messageBottom),
            verticalArrangement = Arrangement.spacedBy(Dimensions.messageGap)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.iconGap)) {
                RoadNoteBadge(tone)
                Text(label, style = Type.messageLabel, color = PleosTheme.colors.textPrimary)
            }
            Text(message, style = if (compact) Type.compactMessage else Type.message,
                color = PleosTheme.colors.textPrimary,
                modifier = if (compact) Modifier.widthIn(max = Dimensions.compactMessageWidth).fillMaxWidth()
                    else Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun RoadNoteInfoCard(label: String, message: String, modifier: Modifier = Modifier) {
    Surface(modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(Dimensions.statusCardRadius), color = PleosTheme.colors.surface) {
        Row(Modifier.fillMaxWidth().padding(Dimensions.statusCardHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.iconGap)) {
            RoadNoteBadge(RoadNoteTone.Information)
            Column(verticalArrangement = Arrangement.spacedBy(PleosSpacing.space05)) {
                Text(label, style = Type.cardTitle, color = PleosTheme.colors.textPrimary)
                Text(message, style = Type.supporting, color = PleosTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
fun RoadNoteStatus(text: String, modifier: Modifier = Modifier, loading: Boolean = false) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(PleosSpacing.space03),
        verticalAlignment = Alignment.CenterVertically) {
        if (loading) CircularProgressIndicator(Modifier.size(PleosSpacing.space05),
            color = PleosTheme.colors.textSecondary)
        Text(text, style = Type.status, color = PleosTheme.colors.textSecondary)
    }
}

// Design's 49px actions differ from the Foundation's minimum 64px basic buttons.
@Composable
fun RoadNoteAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = PleosTheme.colors
    Button(onClick, modifier.widthIn(min = Dimensions.actionMinWidth).heightIn(min = Dimensions.actionHeight),
        enabled = enabled, shape = RoundedCornerShape(Dimensions.actionRadius),
        colors = ButtonDefaults.buttonColors(containerColor = colors.buttonFilled,
            contentColor = colors.buttonFilledText, disabledContainerColor = colors.buttonFilledDisabled,
            disabledContentColor = colors.buttonDisabledText)) {
        Text(text, style = Type.action)
    }
}
