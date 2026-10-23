package hu.merenyimiklos.meterreader.auth

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.merenyimiklos.meterreader.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val configured: Boolean = false,
    val loading: Boolean = false,
    val user: AuthUser? = null,
    val guestMode: Boolean = false,
    val error: String? = null,
    val message: String? = null
) {
    val authenticated: Boolean
        get() =
            user != null ||
                guestMode
}

class AuthViewModel(
    application: Application
) : AndroidViewModel(
    application
) {
    private val repository =
        AuthRepository(
            application
        )

    private val _state =
        MutableStateFlow(
            AuthUiState(
                configured =
                    repository
                        .isConfigured,
                user =
                    repository
                        .currentUser()
            )
        )

    val state:
        StateFlow<AuthUiState> =
        _state.asStateFlow()

    fun signIn(
        email: String,
        password: String
    ) {
        launchAuth {
            repository.signIn(
                email,
                password
            )
        }
    }

    fun register(
        name: String,
        email: String,
        password: String
    ) {
        launchAuth {
            repository.register(
                name,
                email,
                password
            )
        }
    }

    fun sendPasswordReset(
        email: String
    ) {
        if (email.isBlank()) {
            _state.value =
                _state.value.copy(
                    error =
                        "Add meg az e-mail címed."
                )
            return
        }

        viewModelScope.launch {
            setLoading()

            runCatching {
                repository
                    .sendPasswordReset(
                        email
                    )
            }.onSuccess {
                _state.value =
                    _state.value.copy(
                        loading = false,
                        error = null,
                        message =
                            "Elküldtük a jelszó-visszaállító e-mailt."
                    )
            }.onFailure {
                error ->
                setError(error)
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity
    ) {
        launchAuth {
            repository
                .signInWithGoogle(
                    activity
                )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching {
                repository.signOut()
            }

            _state.value =
                AuthUiState(
                    configured =
                        repository
                            .isConfigured
                )
        }
    }

    fun continueOfflineDebug() {
        if (
            BuildConfig.DEBUG &&
            !repository.isConfigured
        ) {
            _state.value =
                _state.value.copy(
                    guestMode = true,
                    error = null,
                    message = null
                )
        }
    }

    fun clearFeedback() {
        _state.value =
            _state.value.copy(
                error = null,
                message = null
            )
    }

    private fun launchAuth(
        action:
            suspend () -> AuthUser
    ) {
        viewModelScope.launch {
            setLoading()

            runCatching {
                action()
            }.onSuccess {
                user ->
                _state.value =
                    _state.value.copy(
                        configured = true,
                        loading = false,
                        user = user,
                        guestMode = false,
                        error = null,
                        message = null
                    )
            }.onFailure {
                error ->
                setError(error)
            }
        }
    }

    private fun setLoading() {
        _state.value =
            _state.value.copy(
                loading = true,
                error = null,
                message = null
            )
    }

    private fun setError(
        error: Throwable
    ) {
        _state.value =
            _state.value.copy(
                loading = false,
                error =
                    friendlyMessage(
                        error
                    ),
                message = null
            )
    }

    private fun friendlyMessage(
        error: Throwable
    ): String {
        val message =
            error.message
                .orEmpty()

        return when {
            message.contains(
                "password",
                ignoreCase = true
            ) &&
                message.contains(
                    "invalid",
                    ignoreCase = true
                ) ->
                "Hibás e-mail cím vagy jelszó."

            message.contains(
                "email",
                ignoreCase = true
            ) &&
                message.contains(
                    "already",
                    ignoreCase = true
                ) ->
                "Ehhez az e-mail címhez már tartozik fiók."

            message.contains(
                "network",
                ignoreCase = true
            ) ->
                "Nincs megfelelő internetkapcsolat."

            message.isNotBlank() ->
                message

            else ->
                "A művelet nem sikerült."
        }
    }
}
