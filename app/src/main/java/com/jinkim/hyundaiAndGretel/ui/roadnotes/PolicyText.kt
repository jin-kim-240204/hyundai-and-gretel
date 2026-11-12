package com.jinkim.hyundaiAndGretel.ui.roadnotes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

@Composable
fun PolicyText(text: String, policy: UiRestrictions, style: TextStyle = TextStyle.Default, maxLines: Int = 3) {
    Text(policy.label(text), style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}
