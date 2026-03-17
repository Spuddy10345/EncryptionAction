package com.example.encryptaction.ui.screen.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.core.crypto.CryptoResult
import com.example.encryptaction.domain.model.User
import com.example.encryptaction.domain.model.UserRole
import com.example.encryptaction.domain.repository.UserRepository
import com.example.encryptaction.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val showCreateUserDialog: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    val uiState = MutableStateFlow(AdminUiState())

    val users: StateFlow<List<User>> = userRepository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun showCreateUserDialog() = uiState.update { it.copy(showCreateUserDialog = true) }
    fun dismissCreateUserDialog() = uiState.update { it.copy(showCreateUserDialog = false) }

    fun createUser(username: String, displayName: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            uiState.update { it.copy(isLoading = true, showCreateUserDialog = false) }
            when (val result = registerUseCase(username, displayName, email, password, role)) {
                is CryptoResult.Success -> uiState.update {
                    it.copy(isLoading = false, message = "User '${result.data.user.username}' created.\nFingerprint: ${result.data.fingerprintForDisplay}")
                }
                is CryptoResult.Failure -> uiState.update {
                    it.copy(isLoading = false, message = "Error: ${result.error.message}")
                }
            }
        }
    }

    fun toggleUserActive(user: User) {
        viewModelScope.launch {
            userRepository.updateUser(user.copy(isActive = !user.isActive))
        }
    }

    fun clearMessage() = uiState.update { it.copy(message = null) }
}
