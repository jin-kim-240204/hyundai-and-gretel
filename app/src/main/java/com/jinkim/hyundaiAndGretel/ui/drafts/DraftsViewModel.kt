package com.jinkim.hyundaiAndGretel.ui.drafts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jinkim.hyundaiAndGretel.roadnote.DraftRepository
import com.jinkim.hyundaiAndGretel.roadnote.DraftSnapshot
import com.jinkim.hyundaiAndGretel.roadnote.RoadNoteDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DraftUiState(
    val consentGranted: Boolean = false,
    val drafts: List<RoadNoteDraft> = emptyList(),
    val busy: Boolean = false,
    val hasError: Boolean = false,
)

@HiltViewModel
class DraftsViewModel @Inject constructor(private val repository: DraftRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(DraftUiState())
    val state = mutableState.asStateFlow()

    /** 저장소의 최신 동의·초안 목록을 불러와 화면 상태에 반영한다. */
    fun refresh() = update { repository.load() }

    /** 저장 동의 변경을 요청하고 변경 후 화면을 갱신한다. */
    fun setConsent(granted: Boolean) = update { repository.setConsent(granted) }

    /** 선택한 초안의 삭제를 요청하고 남은 목록으로 화면을 갱신한다. */
    fun delete(id: String) = update { repository.delete(id) }

    /**
     * 중복 요청을 막고 로딩 → 작업 결과 → 로딩 해제 순서로 StateFlow를 갱신한다.
     * 실패하면 기존 화면 데이터를 유지하고 오류를 표시하며, 취소 시에도 로딩 표시는 해제한다.
     */
    private fun update(operation: suspend () -> Result<DraftSnapshot>) {
        // 처리 중인 요청이 있으면 추가 조작을 무시한다. 화면 버튼도 같은 busy 값으로 비활성화한다.
        if (mutableState.value.busy) return
        mutableState.update { state -> state.copy(busy = true, hasError = false) }

        viewModelScope.launch {
            try {
                val result = operation()
                result.fold(
                    onSuccess = { snapshot ->
                        mutableState.value = DraftUiState(snapshot.consentGranted, snapshot.drafts, busy = true)
                    },
                    onFailure = {
                        mutableState.update { state -> state.copy(hasError = true) }
                    },
                )
            } finally {
                // 오류뿐 아니라 ViewModel 제거에 따른 취소에서도 로딩 표시를 해제한다.
                mutableState.update { state -> state.copy(busy = false) }
            }
        }
    }
}
