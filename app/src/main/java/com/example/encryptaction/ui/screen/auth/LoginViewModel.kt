package com.example.encryptaction.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    val uiState = MutableStateFlow(LoginUiState())

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        if (username.isBlank() || password.isBlank()) {
            uiState.update { it.copy(error = "Username and password are required") }
            return
        }
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = loginUseCase(username, password)) {
                is CryptoResult.Success -> onSuccess()
                is CryptoResult.Failure -> uiState.update {
                    it.copy(isLoading = false, error = result.error.message)
                }
            }
        }
    }

    fun clearError() = uiState.update { it.copy(error = null) }
}
