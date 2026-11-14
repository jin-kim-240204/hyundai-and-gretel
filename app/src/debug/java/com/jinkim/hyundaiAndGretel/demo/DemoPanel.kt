package com.jinkim.hyundaiAndGretel.demo

import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jinkim.hyundaiAndGretel.R
import com.jinkim.hyundaiAndGretel.location.TrackingStatus
import com.jinkim.hyundaiAndGretel.notes.NoteCategory
import com.jinkim.hyundaiAndGretel.notes.submit.SubmissionState
import com.jinkim.hyundaiAndGretel.ui.roadnotes.*
import com.jinkim.hyundaiAndGretel.ui.ux.UiRestrictions

private val demoButtonHeight = 56.dp
private val demoButtonMargin = 8.dp
internal val demoPanelBottomPadding = demoButtonHeight + demoButtonMargin
private val demoDialogMaxWidth = 780.dp
private val demoPanelColor = Color(0xFF181818)
private val demoCardColor = Color(0xFF212121)
private val demoBorder = Color(0xFF535353)
private val demoMuted = Color(0xFFB8B8B8)
private val demoText = Color(0xFFF5F5F5)
private val demoColors = darkColorScheme(
    primary = Color(0xFFF1F1F1), onPrimary = Color(0xFF171717),
    surface = demoPanelColor, onSurface = demoText, onSurfaceVariant = demoMuted,
    outline = demoBorder, secondaryContainer = Color(0xFF303030),
    onSecondaryContainer = demoText,
)

@Composable
fun DemoPanel(productViewModel: RoadNotesViewModel, onStart: () -> Unit, onStop: () -> Unit) {
    val demoViewModel: DemoViewModel = viewModel(factory = viewModelFactory {
        initializer { DemoViewModel(productViewModel.serviceClient.service) }
    })
    val runtime by demoViewModel.runtime.collectAsStateWithLifecycle()
    val tools = runtime ?: return
    val ui by productViewModel.uiState.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (open) {
            DemoDialog(policy = ui.restrictions, onClose = { open = false }) {
                DemoPanelContents(productViewModel, demoViewModel, tools, ui, onStart, onStop)
            }
        } else {
            val label = ui.restrictions.label(stringResource(R.string.debug_tools_open))
            SmallFloatingActionButton(
                onClick = { open = true },
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(demoButtonMargin)
                    .height(demoButtonHeight)
                    .widthIn(min = demoButtonHeight)
                    .semantics { contentDescription = label },
                containerColor = demoText,
                contentColor = Color(0xFF171717),
            ) {
                Box(Modifier.padding(horizontal = 12.dp)) {
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
}

@Composable
private fun DemoDialog(policy: UiRestrictions, onClose: () -> Unit, content: @Composable () -> Unit) {
    val closeBackdrop by rememberUpdatedState(onClose)
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect { window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND) }

        MaterialTheme(colorScheme = demoColors) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.matchParentSize().background(Color.White).pointerInput(Unit) {
                        detectTapGestures(onTap = { closeBackdrop() })
                    },
                )
                // Surface blocks hit testing of the lower backdrop sibling, including blank panel areas.
                Surface(
                    modifier = Modifier.padding(12.dp).widthIn(max = demoDialogMaxWidth).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, demoBorder),
                    shadowElevation = 24.dp,
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                                Box(Modifier.semantics { heading() }) {
                                    PolicyText(stringResource(R.string.debug_tools_title), policy, MaterialTheme.typography.headlineSmall)
                                }
                                DemoCaption(stringResource(R.string.debug_tools_subtitle), policy)
                            }
                            IconButton(onClick = onClose, modifier = Modifier.size(56.dp)) {
                                Icon(
                                    painter = painterResource(R.drawable.debug_close),
                                    contentDescription = policy.label(stringResource(R.string.debug_tools_close)),
                                )
                            }
                        }
                        HorizontalDivider(color = demoBorder)
                        Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DemoPanelContents(
    viewModel: RoadNotesViewModel,
    demoViewModel: DemoViewModel,
    tools: DemoRuntime,
    ui: RoadNotesUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val demo by tools.state.collectAsStateWithLifecycle()
    val notes by tools.notes.collectAsStateWithLifecycle()
    var details by remember { mutableStateOf(false) }
    val policy = ui.restrictions
    val visible = visibleDemoContent(ui, details)
    val settingsAllowed = policy.canSetup && !tools.transitioning && !tools.writesBusy &&
        ui.submission != SubmissionState.Submitting && ui.inbox.confirming == null
    SideEffect { visible.forEach { viewModel.markPresented("debug:$it") } }

    BoxWithConstraints {
        val narrow = maxWidth <= 580.dp
        val inset = if (narrow) 14.dp else 24.dp
        Column(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(horizontal = inset, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DemoCard {
                    FlowRow(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.align(Alignment.CenterVertically),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Surface(
                                    Modifier.size(8.dp).align(Alignment.CenterVertically),
                                    shape = CircleShape,
                                    color = if (ui.trackingStatus.isCollecting) demoText else demoCardColor,
                                    border = BorderStroke(1.dp, demoText),
                                ) {}
                                PolicyText(
                                    stringResource(
                                        if (ui.trackingStatus == TrackingStatus.STOPPED) R.string.debug_tracking_stopped
                                        else ui.trackingStatus.messageRes,
                                    ).replace('·', '/'),
                                    policy, MaterialTheme.typography.titleMedium,
                                )
                                PolicyText(stringResource(demoRoleLabel(demo.actor)), policy)
                            }
                            DemoCaption(stringResource(demoPhaseLabel(demo.phase)), policy)
                            DemoCaption(demo.inputStatus, policy)
                        }
                        DemoAction(stringResource(R.string.debug_tools_stop), policy, onStop)
                    }
                    demo.message?.let { DemoCaption(it, policy) }
                    if (!policy.canSetup) DemoCaption(stringResource(R.string.debug_setup_restricted), policy)
                }

                if ("settings" in visible) {
                    FlowRow(
                        maxItemsInEachRow = if (narrow) 1 else 2,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        DemoCard(Modifier.weight(1f)) {
                            DemoHeading(R.string.debug_settings, policy)
                            DemoCaption(stringResource(R.string.debug_role), policy)
                            DemoRoles(demo.actor, settingsAllowed, policy, demoViewModel::selectActor)
                            DemoCaption(stringResource(R.string.debug_input_source), policy)
                            if (demo.mode == DemoMode.EXTERNAL_ADB) {
                                DemoCaption(stringResource(R.string.debug_mode_external), policy)
                                DemoAction(
                                    stringResource(R.string.debug_switch_console), policy,
                                    { demoViewModel.selectMode(DemoMode.WEB_CONSOLE) },
                                    enabled = settingsAllowed,
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = demoCardColor,
                                    border = BorderStroke(1.dp, demoBorder),
                                ) {
                                    Box(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
                                        PolicyText(stringResource(R.string.debug_mode_console), policy)
                                    }
                                }
                            }
                        }
                        DemoCard(Modifier.weight(1f)) {
                            DemoHeading(R.string.debug_role_actions, policy)
                            DemoAction(
                                stringResource(if (demo.actor == "A") R.string.debug_start_author else R.string.debug_start_recipient),
                                policy, onStart, enabled = settingsAllowed && ui.canStart, primary = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (demo.actor == "A") {
                                val fixture = stringResource(R.string.debug_note_fixture)
                                DemoAction(
                                    stringResource(R.string.debug_note_submit), policy,
                                    { viewModel.submitRoadNote(fixture, NoteCategory.WARNING) },
                                    enabled = tools.canSubmit && ui.canSubmit,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                } else DemoCaption(stringResource(R.string.debug_content_restricted), policy)

                if ("plot" in visible) {
                    DemoCard { DemoRoutePlot(demo, notes, policy) }
                }
                if ("settings" in visible || details) {
                    DemoAction(
                        stringResource(if (details) R.string.debug_details_close else R.string.debug_details_open),
                        policy, { details = !details },
                        prefix = {
                            Icon(
                                painterResource(R.drawable.debug_disclosure),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).rotate(if (details) 90f else 0f),
                            )
                        },
                    )
                }
                if (details && demoDiagnosticKeys.any { it in visible }) {
                    DemoCard { DemoDiagnostics(ui, demo, notes, visible, tools.writesBusy, demoViewModel::bufferSnapshot) }
                }
            }

            if ("settings" in visible) {
                HorizontalDivider(color = demoBorder)
                DemoResetControl(demoViewModel, demo, settingsAllowed, policy, Modifier.padding(horizontal = inset, vertical = 12.dp))
            }
        }
    }
}

private fun demoRoleLabel(actor: String) =
    if (actor == "A") R.string.debug_role_author else R.string.debug_role_recipient

private fun demoPhaseLabel(phase: DemoPhase) = when (phase) {
    DemoPhase.STOPPED -> R.string.debug_phase_stopped
    DemoPhase.PREPARING -> R.string.debug_phase_preparing
    DemoPhase.PROBE -> R.string.debug_phase_probe
    DemoPhase.START -> R.string.debug_phase_start
    DemoPhase.READY -> R.string.debug_phase_ready
    DemoPhase.RESETTING -> R.string.debug_phase_resetting
    DemoPhase.ERROR -> R.string.debug_phase_error
}

@Composable
private fun DemoRoles(actor: String, allowed: Boolean, policy: UiRestrictions, selectActor: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, demoBorder)) {
        Row(Modifier.fillMaxWidth().selectableGroup().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (candidate in listOf("A", "B")) {
                val selected = actor == candidate
                val shape = RoundedCornerShape(8.dp)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = shape,
                    color = if (selected) Color(0xFF414141) else demoPanelColor,
                    contentColor = if (allowed) demoText else demoMuted,
                    border = if (selected) BorderStroke(1.dp, Color(0xFF777777)) else null,
                ) {
                    Box(
                        Modifier.selectable(selected, enabled = allowed, role = Role.RadioButton) {
                            if (!selected) selectActor(candidate)
                        }.sizeIn(minWidth = 56.dp, minHeight = 56.dp).padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PolicyText(stringResource(demoRoleLabel(candidate)), policy, MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun DemoResetControl(viewModel: DemoViewModel, demo: DemoState, allowed: Boolean, policy: UiRestrictions, modifier: Modifier) {
    var confirming by remember(demo.actor, demo.mode, demo.phase, allowed) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            DemoAction(
                stringResource(R.string.debug_reset_open), policy, { confirming = !confirming },
                enabled = allowed,
                prefix = { Icon(painterResource(R.drawable.debug_reset), null, Modifier.size(18.dp)) },
            )
        }
        if (confirming) {
            val confirmation = remember { BringIntoViewRequester() }
            DemoCard(Modifier.bringIntoViewRequester(confirmation)) {
                DemoHeading(R.string.debug_reset_title, policy)
                DemoCaption(stringResource(R.string.debug_reset_description), policy)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DemoAction(stringResource(R.string.debug_reset_cancel), policy, { confirming = false })
                    DemoAction(
                        stringResource(R.string.debug_reset_confirm), policy,
                        { viewModel.resetScenario(); confirming = false }, enabled = allowed, primary = true,
                    )
                }
            }
            LaunchedEffect(confirmation) { confirmation.bringIntoView() }
        }
    }
}

@Composable
private fun DemoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = demoCardColor,
        border = BorderStroke(1.dp, demoBorder),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun DemoHeading(label: Int, policy: UiRestrictions) {
    Box(Modifier.semantics { heading() }) {
        PolicyText(stringResource(label), policy, MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun DemoCaption(text: String, policy: UiRestrictions) {
    CompositionLocalProvider(LocalContentColor provides demoMuted) {
        PolicyText(text, policy, MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DemoAction(
    label: String,
    policy: UiRestrictions,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    prefix: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp),
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, if (primary && enabled) demoColors.primary else demoBorder),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) demoColors.primary else demoColors.secondaryContainer,
            contentColor = if (primary) demoColors.onPrimary else demoText,
            disabledContainerColor = Color(0xFF252525),
            disabledContentColor = Color(0xFFA0A0A0),
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (prefix != null) {
            prefix()
            Spacer(Modifier.width(8.dp))
        }
        PolicyText(label, policy, MaterialTheme.typography.labelLarge)
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
