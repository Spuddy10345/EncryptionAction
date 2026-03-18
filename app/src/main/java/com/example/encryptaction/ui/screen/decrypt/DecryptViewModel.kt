package com.example.encryptaction.ui.screen.decrypt

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.util.toTempFile
import com.example.encryptaction.core.util.resolveDisplayName
import com.example.encryptaction.domain.repository.SessionRepository
import com.example.encryptaction.domain.repository.UserRepository
import com.example.encryptaction.domain.usecase.file.DecryptExternalFileUseCase
import com.example.encryptaction.ui.common.PendingSave
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SenderInfo(
    val displayName: String,
    val username: String,
    val email: String,
    val fingerprint: String,
    val signatureVerified: Boolean,
    val decryptedFileName: String
)

data class DecryptUiState(
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val isLoading: Boolean = false,
    val pendingSave: PendingSave? = null,
    val senderInfo: SenderInfo? = null,
    val message: String? = null
)

@HiltViewModel
class DecryptViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val decryptExternalFile: DecryptExternalFileUseCase
) : ViewModel() {

    val uiState = MutableStateFlow(DecryptUiState())

    fun onFilePicked(uri: Uri, displayName: String) {
        uiState.update { it.copy(selectedFileUri = uri, selectedFileName = displayName) }
    }

    fun decrypt() {
        val uri = uiState.value.selectedFileUri ?: return

        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, message = null, senderInfo = null) }

            val session = sessionRepository.getSession()
            if (session == null) {
                uiState.update { it.copy(isLoading = false, message = "Not logged in") }
                return@launch
            }

            val tempFile = uri.toTempFile(context)
            if (tempFile == null) {
                uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Failed to read selected file. If using Google Drive, ensure the file is available offline."
                    )
                }
                return@launch
            }

            val result = try {
                decryptExternalFile(
                    packageFilePath = tempFile.absolutePath,
                    recipientUserId = session.userId
                )
            } finally {
                tempFile.delete()
            }

            when (result) {
                is CryptoResult.Success -> {
                    val data = result.data
                    val sender = userRepository.getUserById(data.senderKeyId)
                    uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingSave = PendingSave(
                                sourceFilePath = data.outputFilePath,
                                suggestedName = data.suggestedName
                            ),
                            senderInfo = SenderInfo(
                                displayName = sender?.displayName ?: data.senderKeyId,
                                username = sender?.username ?: data.senderKeyId,
                                email = sender?.email ?: "—",
                                fingerprint = sender?.keyBundle?.fingerprint ?: "—",
                                signatureVerified = data.signatureVerified,
                                decryptedFileName = data.suggestedName
                            )
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
            uiState.update {
                it.copy(
                    pendingSave = null,
                    senderInfo = null,
                    message = if (success) "File saved successfully."
                    else "Save failed. File is in app storage at:\n${pending.sourceFilePath}"
                )
            }
        }
    }

    fun onSavePickerDismissed() {
        val pending = uiState.value.pendingSave ?: return
        uiState.update {
            it.copy(
                pendingSave = null,
                senderInfo = null,
                message = "File stored in app storage:\n${pending.sourceFilePath}"
            )
        }
    }

    fun clearSenderInfo() = uiState.update { it.copy(senderInfo = null, pendingSave = null) }

    fun clearMessage() = uiState.update { it.copy(message = null) }
}
