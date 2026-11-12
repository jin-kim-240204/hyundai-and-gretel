package com.jinkim.hyundaiAndGretel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.jinkim.hyundaiAndGretel.gleo.GleoProbeRoute
import com.jinkim.hyundaiAndGretel.ui.drafts.DraftRoute
import com.jinkim.hyundaiAndGretel.ui.drafts.DraftsViewModel
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** 빌드 종류에 맞는 진단/초안 화면을 선택하고 Compose 화면을 시작한다. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val drafts = if (BuildConfig.DEBUG) null else ViewModelProvider(this)[DraftsViewModel::class.java]

        setContent {
            HyundaiAndGretelTheme {
                if (drafts == null) GleoProbeRoute() else DraftRoute(drafts)
            }
        }
    }
}
