package com.jinkim.hyundaiAndGretel.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.roadnotes.*
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

@Composable
fun DemoPanel(productViewModel: RoadNotesViewModel, onStart: () -> Unit, onStop: () -> Unit) {
    val demoViewModel: DemoViewModel = viewModel(factory = viewModelFactory {
        initializer { DemoViewModel(productViewModel.serviceClient.service) }
    })
    val runtime by demoViewModel.runtime.collectAsStateWithLifecycle()
    val tools = runtime ?: return
    val ui by productViewModel.uiState.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (open) {
            val dialogMaxWidth = minOf(maxWidth * 0.9f, 480.dp)
            val dialogMaxHeight = maxHeight * 0.9f
            val close = { open = false }
            Dialog(
                onDismissRequest = close,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(
                    modifier = Modifier.width(dialogMaxWidth).heightIn(max = dialogMaxHeight),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 8.dp,
                ) {
                    DemoPanelContents(productViewModel, demoViewModel, tools, ui, onStart, onStop, onClose = close)
                }
            }
        } else {
            val label = ui.restrictions.label(stringResource(R.string.debug_tools_open))
            SmallFloatingActionButton(
                onClick = { open = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = label },
                containerColor = Color(0xFFF5C451),
                contentColor = Color(0xFF171717),
            ) {
                PolicyText(
                    stringResource(R.string.debug_tools_badge),
                    ui.restrictions,
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoPanelContents(viewModel: RoadNotesViewModel, demoViewModel: DemoViewModel, tools: DemoRuntime, ui: RoadNotesUiState, onStart: () -> Unit, onStop: () -> Unit, onClose: () -> Unit) {
    val demo by tools.state.collectAsStateWithLifecycle()
    val notes by tools.notes.collectAsStateWithLifecycle()
    var details by remember { mutableStateOf(false) }
    val policy = ui.restrictions
    val visible = visibleDemoContent(ui, details)
    val settingsAllowed = policy.canSetup && !demo.transitioning && !tools.writesBusy &&
        ui.submission != SubmissionState.Submitting && ui.inbox.confirming == null
    SideEffect { visible.forEach { viewModel.markPresented("debug:$it") } }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PolicyText(stringResource(R.string.debug_tools_title), policy, style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onClose, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.debug_tools_close), policy) }
            Button(onClick = onStop, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.debug_tools_stop), policy) }
        }
        val role = stringResource(if (demo.actor == "A") R.string.debug_role_author else R.string.debug_role_recipient)
        PolicyText(stringResource(R.string.debug_session_status, role, demo.mode, demo.phase), policy)
        PolicyText(demo.inputStatus, policy)
        demo.message?.let { PolicyText(it, policy) }
        if (!policy.canSetup) PolicyText(stringResource(R.string.debug_setup_restricted), policy)

        if ("settings" in visible) {
            DemoRoleAndInputControls(demoViewModel, demo, settingsAllowed, ui.canStart, onStart, policy)
            if (demo.actor == "A") {
                val content = stringResource(R.string.debug_note_fixture)
                Button(
                    onClick = { viewModel.submitRoadNote(content, NoteCategory.WARNING) },
                    enabled = tools.canSubmit && ui.canSubmit,
                    modifier = Modifier.heightIn(min = 56.dp),
                ) { PolicyText(stringResource(R.string.debug_note_submit), policy) }
            }
            DemoResetControl(demoViewModel, demo, settingsAllowed, policy)
            Button(onClick = { details = !details }, modifier = Modifier.heightIn(min = 56.dp)) {
                PolicyText(stringResource(if (details) R.string.debug_details_close else R.string.debug_details_open), policy)
            }
        } else PolicyText(stringResource(R.string.debug_content_restricted), policy)

        if (details) DemoDiagnostics(ui, demo, notes, visible, tools.writesBusy, demoViewModel::bufferSnapshot)
        if ("plot" in visible) DemoRoutePlot(demo, notes, policy)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoRoleAndInputControls(viewModel: DemoViewModel, demo: DemoState, allowed: Boolean, canStart: Boolean, onStart: () -> Unit, policy: UiRestrictions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { viewModel.selectActor("A") }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.debug_role_author), policy) }
        Button(onClick = { viewModel.selectActor("B") }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.debug_role_recipient), policy) }
    }
    Button(onClick = { viewModel.selectMode(DemoMode.WEB_CONSOLE) }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.debug_mode_console), policy) }
    Button(onClick = { viewModel.selectMode(DemoMode.EXTERNAL_ADB) }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.debug_mode_external), policy) }
    Button(onClick = onStart, enabled = allowed && canStart, modifier = Modifier.heightIn(min = 56.dp)) {
        PolicyText(stringResource(if (demo.actor == "A") R.string.debug_start_author else R.string.debug_start_recipient), policy)
    }
}

@Composable
private fun DemoResetControl(viewModel: DemoViewModel, demo: DemoState, allowed: Boolean, policy: UiRestrictions) {
    var confirming by remember(demo.actor, demo.mode, demo.phase) { mutableStateOf(false) }
    Button(onClick = { confirming = true }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.debug_reset_open), policy) }
    if (confirming) {
        PolicyText(stringResource(R.string.debug_reset_description), policy)
        Button(onClick = { viewModel.resetScenario(); confirming = false }, enabled = allowed, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.debug_reset_confirm), policy) }
        Button(onClick = { confirming = false }, modifier = Modifier.heightIn(min = 56.dp)) { PolicyText(stringResource(R.string.debug_reset_cancel), policy) }
    }
}

internal fun visibleDemoContent(ui: RoadNotesUiState, details: Boolean): Set<String> {
    val policy = ui.restrictions
    var presented = ui.visitedContent
    ui.inbox.pending?.let { item ->
        val key = "receipt:${item.recipientId}/${item.note.noteId}"
        if (policy.canShowBody && policy.fits(item.note.receiverMessage) && policy.allowsContent(key, presented)) presented += key
    }
    val sections = buildList {
        add("settings")
        if (details) addAll(demoDiagnosticKeys)
        if (policy.canShowPlot) add("plot")
    }
    return sections.filterTo(linkedSetOf()) { key ->
        val id = "debug:$key"
        policy.allowsContent(id, presented, depth = 2).also { allowed -> if (allowed) presented += id }
    }
}
