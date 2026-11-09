package com.jinkim.hyundaiAndGretel.ui.notes

import androidx.compose.runtime.Composable
import com.jinkim.hyundaiAndGretel.demo.DemoState
import com.jinkim.hyundaiAndGretel.notes.Actor

@Composable
fun DebugPanel(state: DemoState, onActor: (Actor) -> Unit, onReset: () -> Unit, onReport: (String) -> Unit, onStartPosition: (Actor) -> Unit, onExternalGps: () -> Unit) = Unit
