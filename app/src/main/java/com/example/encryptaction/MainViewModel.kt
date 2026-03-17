package com.example.encryptaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.encryptaction.domain.model.Session
import com.example.encryptaction.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    sessionRepository: SessionRepository
) : ViewModel() {

    val session: StateFlow<Session?> = sessionRepository.observeSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
