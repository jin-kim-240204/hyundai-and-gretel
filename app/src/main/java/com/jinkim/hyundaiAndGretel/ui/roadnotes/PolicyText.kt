package com.jinkim.hyundaiAndGretel.ui.roadnotes

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

/** 화면 문구에는 플랫폼 글자 수 제한을 적용하고 저장된 원문은 변경하지 않는다. */
@Composable
fun PolicyText(text: String, policy: UiRestrictions, style: TextStyle = TextStyle.Default, maxLines: Int = 3) {
    Text(policy.label(text), style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}
