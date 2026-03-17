package com.example.encryptaction.ui.screen.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.domain.model.UserKeyBundle
import com.example.encryptaction.domain.repository.SessionRepository
import com.example.encryptaction.domain.repository.UserRepository
import com.example.encryptaction.domain.usecase.key.ExportPublicKeyUseCase
import com.example.encryptaction.domain.usecase.key.ImportPublicKeyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeysUiState(
    val exportedPem: String? = null,
    val showImportDialog: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class KeysViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val exportKey: ExportPublicKeyUseCase,
    private val importKey: ImportPublicKeyUseCase
) : ViewModel() {

    val uiState = MutableStateFlow(KeysUiState())

    val keyBundles: StateFlow<List<UserKeyBundle>> = userRepository.observeKeyBundles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun exportMyKeys() {
        viewModelScope.launch {
            val session = sessionRepository.getSession() ?: return@launch
            when (val result = exportKey(session.userId)) {
                is CryptoResult.Success -> uiState.update { it.copy(exportedPem = result.data) }
                is CryptoResult.Failure -> uiState.update { it.copy(message = "Export failed: ${result.error.message}") }
            }
        }
    }

    fun showImportDialog() = uiState.update { it.copy(showImportDialog = true) }
    fun dismissImportDialog() = uiState.update { it.copy(showImportDialog = false) }
    fun dismissExport() = uiState.update { it.copy(exportedPem = null) }

    fun importKeys(userId: String, pemText: String) {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, showImportDialog = false) }
            when (val result = importKey(userId, pemText)) {
                is CryptoResult.Success -> uiState.update {
                    it.copy(isLoading = false, message = "Keys imported for user $userId\nFingerprint: ${result.data.fingerprint}")
                }
                is CryptoResult.Failure -> uiState.update {
                    it.copy(isLoading = false, message = "Import failed: ${result.error.message}")
                }
            }
        }
    }

    fun clearMessage() = uiState.update { it.copy(message = null) }
}
