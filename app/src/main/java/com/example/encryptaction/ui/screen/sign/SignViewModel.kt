package com.example.encryptaction.ui.screen.sign

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.core.util.resolveDisplayName
import com.example.encryptaction.core.util.toTempFile
import com.example.encryptaction.domain.model.SignedFile
import com.example.encryptaction.domain.repository.FileRepository
import com.example.encryptaction.domain.repository.SessionRepository
import com.example.encryptaction.domain.usecase.file.SignFileUseCase
import com.example.encryptaction.domain.usecase.file.VerifyFileUseCase
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

data class SignUiState(
    // Sign tab
    val signFileUri: Uri? = null,
    val signFileName: String? = null,
    // Verify tab
    val verifyFileUri: Uri? = null,
    val verifyFileName: String? = null,
    val verifySigUri: Uri? = null,
    val verifySigFileName: String? = null,
    // Common
    val isLoading: Boolean = false,
    val pendingSave: PendingSave? = null,
    val message: String? = null
)

@HiltViewModel
class SignViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val fileRepository: FileRepository,
    private val signFile: SignFileUseCase,
    private val verifyFile: VerifyFileUseCase
) : ViewModel() {

    val uiState = MutableStateFlow(SignUiState())

    val signedFiles: StateFlow<List<SignedFile>> = sessionRepository.observeSession()
        .flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else fileRepository.observeSignedFiles(session.userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSignFilePicked(uri: Uri) {
        uiState.update { it.copy(signFileUri = uri, signFileName = uri.resolveDisplayName(context)) }
    }

    fun onVerifyFilePicked(uri: Uri) {
        uiState.update { it.copy(verifyFileUri = uri, verifyFileName = uri.resolveDisplayName(context)) }
    }

    fun onVerifySigFilePicked(uri: Uri) {
        uiState.update { it.copy(verifySigUri = uri, verifySigFileName = uri.resolveDisplayName(context)) }
    }

    fun sign() {
        val uri = uiState.value.signFileUri ?: return
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, message = null) }
            val session = sessionRepository.getSession()
            if (session == null) {
                uiState.update { it.copy(isLoading = false, message = "Not logged in") }
                return@launch
            }
            val tempFile = uri.toTempFile(context)
            if (tempFile == null) {
                uiState.update { it.copy(isLoading = false, message = "Failed to read file. If using Google Drive, ensure the file is available offline.") }
                return@launch
            }
            val result = try {
                signFile(tempFile.absolutePath, session.userId)
            } finally {
                tempFile.delete()
            }
            when (result) {
                is CryptoResult.Success -> uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingSave = PendingSave(
                            sourceFilePath = result.data.signatureFilePath,
                            suggestedName = File(result.data.signatureFilePath).name
                        )
                    )
                }
                is CryptoResult.Failure -> uiState.update {
                    it.copy(isLoading = false, message = "Error: ${result.error.message}")
                }
            }
        }
    }

    fun verify() {
        val fileUri = uiState.value.verifyFileUri ?: return
        val sigUri = uiState.value.verifySigUri ?: return
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, message = null) }
            val session = sessionRepository.getSession()
            if (session == null) {
                uiState.update { it.copy(isLoading = false, message = "Not logged in") }
                return@launch
            }
            val fileTemp = fileUri.toTempFile(context)
            val sigTemp = sigUri.toTempFile(context)
            if (fileTemp == null || sigTemp == null) {
                fileTemp?.delete(); sigTemp?.delete()
                uiState.update { it.copy(isLoading = false, message = "Failed to read file(s). If using Google Drive, ensure files are available offline.") }
                return@launch
            }
            val result = try {
                verifyFile(
                    filePath = fileTemp.absolutePath,
                    signatureFilePath = sigTemp.absolutePath,
                    expectedSignerUserId = session.userId
                )
            } finally {
                fileTemp.delete(); sigTemp.delete()
            }
            when (result) {
                is CryptoResult.Success -> uiState.update {
                    it.copy(isLoading = false, message = result.data.message)
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
                    message = if (success) "Signature file saved successfully!"
                    else "Save failed. Signature stored at:\n${pending.sourceFilePath}"
                )
            }
        }
    }

    fun onSavePickerDismissed() {
        val pending = uiState.value.pendingSave ?: return
        uiState.update {
            it.copy(
                pendingSave = null,
                message = "Signature stored in app storage at:\n${pending.sourceFilePath}"
            )
        }
    }

    fun clearMessage() = uiState.update { it.copy(message = null) }
}
