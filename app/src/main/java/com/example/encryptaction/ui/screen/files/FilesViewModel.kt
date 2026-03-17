package com.example.encryptaction.ui.screen.files

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.domain.model.EncryptedFile
import com.example.encryptaction.domain.repository.SessionRepository
import com.example.encryptaction.domain.usecase.file.DecryptFileUseCase
import com.example.encryptaction.ui.common.PendingSave
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import com.example.encryptaction.domain.repository.FileRepository

data class FilesUiState(
    val isLoading: Boolean = false,
    val pendingSave: PendingSave? = null,
    val message: String? = null
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val fileRepository: FileRepository,
    private val decryptFile: DecryptFileUseCase
) : ViewModel() {

    val uiState = MutableStateFlow(FilesUiState())

    val encryptedFiles: StateFlow<List<EncryptedFile>> = sessionRepository.observeSession()
        .flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else fileRepository.observeEncryptedFiles(session.userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun decrypt(fileId: String) {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, message = null) }
            val session = sessionRepository.getSession()
            if (session == null) {
                uiState.update { it.copy(isLoading = false, message = "Not logged in") }
                return@launch
            }
            when (val result = decryptFile(fileId, session.userId)) {
                is CryptoResult.Success -> {
                    val sigInfo = if (result.data.signatureVerified) "✓ Signature verified" else "⚠ Signature not verified"
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingSave = PendingSave(
                                sourceFilePath = result.data.outputFilePath,
                                suggestedName = result.data.originalName
                            ),
                            message = sigInfo
                        )
                    }
                }
                is CryptoResult.Failure -> uiState.update {
                    it.copy(isLoading = false, message = "Error: ${result.error.message}")
                }
            }
        }
    }

    fun saveToUri(destUri: Uri) {
        val pending = uiState.value.pendingSave ?: return
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        File(pending.sourceFilePath).inputStream().use { it.copyTo(out) }
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            val sigInfo = uiState.value.message ?: ""
            uiState.update {
                it.copy(
                    pendingSave = null,
                    message = if (success) "Saved successfully!\n$sigInfo".trim()
                    else "Save failed. File stored at:\n${pending.sourceFilePath}\n$sigInfo".trim()
                )
            }
        }
    }

    fun onSavePickerDismissed() {
        val pending = uiState.value.pendingSave ?: return
        val sigInfo = uiState.value.message ?: ""
        uiState.update {
            it.copy(
                pendingSave = null,
                message = "File stored in app storage at:\n${pending.sourceFilePath}\n$sigInfo".trim()
            )
        }
    }

    fun clearMessage() = uiState.update { it.copy(message = null) }
}
