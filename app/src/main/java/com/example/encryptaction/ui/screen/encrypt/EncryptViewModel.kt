package com.example.encryptaction.ui.screen.encrypt

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.util.resolveMimeType
import com.example.encryptaction.core.util.toTempFile
import com.example.encryptaction.domain.model.User
import com.example.encryptaction.domain.repository.SessionRepository
import com.example.encryptaction.domain.repository.UserRepository
import com.example.encryptaction.domain.usecase.file.EncryptFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.encryptaction.ui.common.PendingSave
import javax.inject.Inject

data class EncryptUiState(
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val selectedRecipient: User? = null,
    val isLoading: Boolean = false,
    val pendingSave: PendingSave? = null,
    val message: String? = null
)

@HiltViewModel
class EncryptViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val encryptFile: EncryptFileUseCase
) : ViewModel() {

    val uiState = MutableStateFlow(EncryptUiState())

    val users: StateFlow<List<User>> = userRepository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onFilePicked(uri: Uri, displayName: String) {
        uiState.update { it.copy(selectedFileUri = uri, selectedFileName = displayName) }
    }

    fun onRecipientSelected(user: User) {
        uiState.update { it.copy(selectedRecipient = user) }
    }

    fun encrypt() {
        val state = uiState.value
        val uri = state.selectedFileUri ?: return
        val recipient = state.selectedRecipient ?: return

        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, message = null) }

            val session = sessionRepository.getSession()
            if (session == null) {
                uiState.update { it.copy(isLoading = false, message = "Not logged in") }
                return@launch
            }

            val tempFile = uri.toTempFile(context)
            if (tempFile == null) {
                uiState.update { it.copy(isLoading = false, message = "Failed to read selected file. If using Google Drive, ensure the file is available offline.") }
                return@launch
            }

            val mimeType = uri.resolveMimeType(context)

            val result = try {
                encryptFile(
                    sourceFilePath = tempFile.absolutePath,
                    mimeType = mimeType,
                    senderUserId = session.userId,
                    recipientUserId = recipient.id
                )
            } finally {
                tempFile.delete()
            }

            when (result) {
                is CryptoResult.Success -> uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingSave = PendingSave(
                            sourceFilePath = result.data.encryptedFilePath,
                            suggestedName = "${result.data.originalName}.eaep"
                        )
                    )
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
            uiState.update {
                it.copy(
                    pendingSave = null,
                    message = if (success) "Saved successfully!" else "Save failed. Package is stored at:\n${pending.sourceFilePath}"
                )
            }
        }
    }

    fun onSavePickerDismissed() {
        val pending = uiState.value.pendingSave ?: return
        uiState.update {
            it.copy(
                pendingSave = null,
                message = "Package stored in app storage at:\n${pending.sourceFilePath}"
            )
        }
    }

    fun clearMessage() = uiState.update { it.copy(message = null) }
}
