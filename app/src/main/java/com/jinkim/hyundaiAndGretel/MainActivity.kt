package com.jinkim.hyundaiAndGretel

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jinkim.hyundaiAndGretel.gleo.GleoProbe
import com.jinkim.hyundaiAndGretel.gleo.GleoProbeScreen
import com.jinkim.hyundaiAndGretel.roadnote.LocalDraftStore
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteDraft
import com.jinkim.hyundaiAndGretel.ui.drafts.DraftScreen
import com.jinkim.hyundaiAndGretel.ui.theme.HyundaiAndGretelTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var drafts: DraftsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) drafts = ViewModelProvider(this)[DraftsViewModel::class.java]
        setContent {
            HyundaiAndGretelTheme {
                if (BuildConfig.DEBUG) {
                    val events = GleoProbe.events.collectAsStateWithLifecycle().value
                    GleoProbeScreen(events, GleoProbe::clear)
                } else {
                    val state = drafts.state.collectAsStateWithLifecycle().value
                    DraftScreen(
                        consentGranted = state.consentGranted,
                        drafts = state.drafts,
                        busy = state.busy,
                        errorMessage = state.errorMessage,
                        onConsentChange = drafts::setConsent,
                        onDelete = drafts::delete,
                        onRefresh = drafts::refresh,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::drafts.isInitialized) drafts.refresh()
    }
}

data class DraftUiState(
    val consentGranted: Boolean = false,
    val drafts: List<RoadNoteDraft> = emptyList(),
    val busy: Boolean = false,
    val errorMessage: String? = null,
)

class DraftsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalDraftStore(application)
    private val mutableState = MutableStateFlow(DraftUiState())
    val state = mutableState.asStateFlow()

    fun refresh() = update { }
    fun setConsent(granted: Boolean) = update { store.setConsent(granted) }
    fun delete(id: String) = update { store.delete(id) }

    private fun update(operation: suspend () -> Unit) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, errorMessage = null)
        viewModelScope.launch {
            try {
                operation()
                val consent = store.hasConsent()
                val drafts = if (consent) store.list() else emptyList()
                mutableState.value = DraftUiState(consent, drafts)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    busy = false,
                    errorMessage = "초안을 처리하지 못했습니다. 다시 시도해 주세요.",
                )
            } finally {
                mutableState.value = mutableState.value.copy(busy = false)
            }
        }
    }
}
