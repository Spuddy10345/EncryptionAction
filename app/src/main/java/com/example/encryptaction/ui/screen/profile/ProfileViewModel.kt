package com.example.encryptaction.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.domain.model.Session
import com.example.encryptaction.domain.model.User
import com.example.encryptaction.domain.repository.SessionRepository
import com.example.encryptaction.domain.repository.UserRepository
import com.example.encryptaction.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val session: Session? = null,
    val user: User? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    val uiState = MutableStateFlow(ProfileUiState())

    init {
        viewModelScope.launch {
            val session = sessionRepository.getSession()
            if (session != null) {
                val user = userRepository.getUserById(session.userId)
                uiState.update { it.copy(session = session, user = user) }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onComplete()
        }
    }
}
