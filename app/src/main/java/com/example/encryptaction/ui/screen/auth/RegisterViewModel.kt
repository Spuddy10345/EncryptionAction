package com.example.encryptaction.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    val uiState = MutableStateFlow(RegisterUiState())

    fun register(username: String, displayName: String, email: String, password: String, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            uiState.update { it.copy(error = "Username and password are required") }
            return
        }
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = registerUseCase(username, displayName, email, password)) {
                is CryptoResult.Success -> onSuccess()
                is CryptoResult.Failure -> uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun clearError() = uiState.update { it.copy(error = null) }
}
