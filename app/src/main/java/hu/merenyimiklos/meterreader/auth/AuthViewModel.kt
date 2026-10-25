package hu.merenyimiklos.meterreader.auth

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.merenyimiklos.meterreader.BuildConfig
import hu.merenyimiklos.meterreader.reminder.ReadingReminderWorker
import hu.merenyimiklos.meterreader.widget.MeterWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val configured: Boolean = false,
    val loading: Boolean = false,
    val user: AuthUser? = null,
    val guestMode: Boolean = false,
    val showOnboarding: Boolean = false,
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
    private val onboardingPreferences =
        application.getSharedPreferences(
            "meterreader_onboarding",
            Application.MODE_PRIVATE
        )

    private val initialUser =
        repository.currentUser()

    private val _state =
        MutableStateFlow(
            AuthUiState(
                configured =
                    repository
                        .isConfigured,
                user =
                    initialUser,
                showOnboarding =
                    !onboardingPreferences
                        .getBoolean(
                            ONBOARDING_COMPLETED_KEY,
                            false
                        )
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
                ReadingReminderWorker.evaluateAndNotify(getApplication())
                MeterWidgetProvider.updateAll(getApplication())
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

    fun completeOnboarding() {
        onboardingPreferences
            .edit()
            .putBoolean(
                ONBOARDING_COMPLETED_KEY,
                true
            )
            .apply()

        _state.value =
            _state.value.copy(
                showOnboarding = false
            )
    }

    fun replayOnboarding() {
        _state.value =
            _state.value.copy(
                showOnboarding = true
            )
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
                        showOnboarding = false,
                        error = null,
                        message = null
                    )
                MeterWidgetProvider.updateAll(getApplication())
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
        val className =
            error::class.simpleName
                .orEmpty()

        val signingSha1 =
            repository
                .signingCertificateSha1()

        return when {
            className.contains(
                "Cancellation",
                ignoreCase = true
            ) ->
                "A Google-belépést megszakítottad."

            className.contains(
                "GetCredential",
                ignoreCase = true
            ) ->
                "A Google-belépés nem indult el. Ellenőrizd a Firebase Google providert és add hozzá ezt az SHA-1-et: " +
                    (signingSha1 ?: "ismeretlen")

            message.contains(
                "DEVELOPER_ERROR",
                ignoreCase = true
            ) ||
                message.contains(
                    "12500",
                    ignoreCase = true
                ) ||
                message.contains(
                    "10:",
                    ignoreCase = true
                ) ->
                "Google-bejelentkezési konfigurációs hiba. Firebase-ben ellenőrizd a Google providert és ezt az SHA-1-et: " +
                    (signingSha1 ?: "ismeretlen")

            message.contains(
                "OAuth",
                ignoreCase = true
            ) ||
                message.contains(
                    "default_web_client_id",
                    ignoreCase = true
                ) ->
                "Hiányzik vagy elavult a Google OAuth konfiguráció. Engedélyezd a Google belépést Firebase-ben, majd töltsd le újra a google-services.json fájlt."


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

    private companion object {
        const val ONBOARDING_COMPLETED_KEY =
            "intro_completed_v1"
    }
}
