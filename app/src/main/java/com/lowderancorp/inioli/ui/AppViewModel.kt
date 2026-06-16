package com.lowderancorp.inioli.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lowderancorp.inioli.data.auth.AuthRepository
import com.lowderancorp.inioli.data.auth.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: UserSession) : SessionState
}

class AppViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    val sessionState: StateFlow<SessionState> =
        authRepository.session
            .map< UserSession?, SessionState> { session ->
                if (session == null) {
                    SessionState.LoggedOut
                } else {
                    SessionState.LoggedIn(session)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SessionState.Loading
            )

    fun onUsernameChange(username: String) {
        _loginUiState.update { state ->
            state.copy(
                username = username,
                errorMessage = null
            )
        }
    }

    fun onPasswordChange(password: String) {
        _loginUiState.update { state ->
            state.copy(
                password = password,
                errorMessage = null
            )
        }
    }

    fun login() {
        val currentState = _loginUiState.value
        val trimmedUsername = currentState.username.trim()

        when {
            currentState.isLoading -> return
            trimmedUsername.isBlank() -> showError("Username is required.")
            currentState.password.isBlank() -> showError("Password is required.")
            else -> {
                _loginUiState.update { it.copy(isLoading = true, errorMessage = null) }
                viewModelScope.launch {
                    try {
                        authRepository.login(
                            username = trimmedUsername,
                            password = currentState.password
                        )
                        _loginUiState.update {
                            it.copy(
                                username = trimmedUsername,
                                password = "",
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    } catch (exception: Throwable) {
                        if (exception is CancellationException) throw exception
                        _loginUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.toAuthUserMessage()
                            )
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginUiState.update {
                it.copy(
                    password = "",
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    private fun showError(message: String) {
        _loginUiState.update { state ->
            state.copy(errorMessage = message)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel(
                    authRepository = inioliApplication().container.authRepository
                )
            }
        }
    }
}
