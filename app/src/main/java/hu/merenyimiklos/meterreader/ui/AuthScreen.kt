package hu.merenyimiklos.meterreader.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import hu.merenyimiklos.meterreader.BuildConfig
import hu.merenyimiklos.meterreader.R
import hu.merenyimiklos.meterreader.auth.AuthUiState
import hu.merenyimiklos.meterreader.auth.AuthViewModel

private enum class AuthMode {
    LOGIN,
    REGISTER,
    RESET
}

@Composable
fun AuthGate(
    authViewModel: AuthViewModel,
    content: @Composable () -> Unit
) {
    val state by
        authViewModel.state
            .collectAsStateWithLifecycle()

    AnimatedContent(
        targetState =
            state.authenticated,
        transitionSpec = {
            fadeIn() togetherWith
                fadeOut()
        },
        label = "auth-gate"
    ) { authenticated ->
        if (authenticated) {
            content()
        } else {
            AuthScreen(
                state = state,
                authViewModel =
                    authViewModel
            )
        }
    }
}

@Composable
private fun AuthScreen(
    state: AuthUiState,
    authViewModel: AuthViewModel
) {
    val context =
        androidx.compose.ui.platform
            .LocalContext.current
    val activity =
        remember(context) {
            context.findActivity()
        }

    var mode by
        remember {
            mutableStateOf(
                AuthMode.LOGIN
            )
        }
    var name by
        remember {
            mutableStateOf("")
        }
    var email by
        remember {
            mutableStateOf("")
        }
    var password by
        remember {
            mutableStateOf("")
        }
    var passwordAgain by
        remember {
            mutableStateOf("")
        }
    var passwordVisible by
        remember {
            mutableStateOf(false)
        }
    var localError by
        remember {
            mutableStateOf<String?>(
                null
            )
        }

    val composition by
        rememberLottieComposition(
            LottieCompositionSpec.RawRes(
                R.raw.auth_energy
            )
        )
    val progress by
        animateLottieCompositionAsState(
            composition = composition,
            iterations =
                LottieConstants.IterateForever
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme
                                .colorScheme
                                .primaryContainer,
                            MaterialTheme
                                .colorScheme
                                .surface,
                            MaterialTheme
                                .colorScheme
                                .secondaryContainer
                        )
                    )
                )
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 20.dp,
                        vertical = 32.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            LottieAnimation(
                composition =
                    composition,
                progress = {
                    progress
                },
                modifier =
                    Modifier.size(
                        170.dp
                    )
            )

            Text(
                text = "Mérőóra Napló",
                style =
                    MaterialTheme
                        .typography
                        .headlineLarge,
                fontWeight =
                    FontWeight.Bold
            )
            Text(
                text =
                    t("A család közös energia-naplója"),
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                Modifier.height(
                    22.dp
                )
            )

            ElevatedCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            20.dp
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            14.dp
                        )
                ) {
                    if (!state.configured) {
                        FirebaseSetupContent(
                            authViewModel =
                                authViewModel
                        )
                    } else {
                        AnimatedContent(
                            targetState =
                                mode,
                            transitionSpec = {
                                (
                                    fadeIn() +
                                        slideInHorizontally {
                                            it / 6
                                        }
                                    ) togetherWith
                                    (
                                        fadeOut() +
                                            slideOutHorizontally {
                                                -it / 6
                                            }
                                        )
                            },
                            label =
                                "auth-mode"
                        ) {
                            currentMode ->
                            Column(
                                verticalArrangement =
                                    Arrangement
                                        .spacedBy(
                                            12.dp
                                        )
                            ) {
                                Text(
                                    text =
                                        when (
                                            currentMode
                                        ) {
                                            AuthMode.LOGIN ->
                                                t("Üdv újra!")

                                            AuthMode.REGISTER ->
                                                t("Hozd létre a fiókod")

                                            AuthMode.RESET ->
                                                t("Jelszó visszaállítása")
                                        },
                                    style =
                                        MaterialTheme
                                            .typography
                                            .headlineSmall,
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                if (
                                    currentMode ==
                                    AuthMode.REGISTER
                                ) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = {
                                            name = it
                                        },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(),
                                        label = {
                                            Text(
                                                t("Név")
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription =
                                                    null
                                            )
                                        },
                                        singleLine =
                                            true
                                    )
                                }

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
                                    label = {
                                        Text(
                                            t("E-mail cím")
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription =
                                                null
                                        )
                                    },
                                    keyboardOptions =
                                        KeyboardOptions(
                                            keyboardType =
                                                KeyboardType.Email
                                        ),
                                    singleLine =
                                        true
                                )

                                if (
                                    currentMode !=
                                    AuthMode.RESET
                                ) {
                                    OutlinedTextField(
                                        value =
                                            password,
                                        onValueChange = {
                                            password =
                                                it
                                        },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(),
                                        label = {
                                            Text(
                                                t("Jelszó")
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription =
                                                    null
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    passwordVisible =
                                                        !passwordVisible
                                                }
                                            ) {
                                                Icon(
                                                    if (
                                                        passwordVisible
                                                    ) {
                                                        Icons.Default.VisibilityOff
                                                    } else {
                                                        Icons.Default.Visibility
                                                    },
                                                    contentDescription =
                                                        null
                                                )
                                            }
                                        },
                                        visualTransformation =
                                            if (
                                                passwordVisible
                                            ) {
                                                VisualTransformation.None
                                            } else {
                                                PasswordVisualTransformation()
                                            },
                                        keyboardOptions =
                                            KeyboardOptions(
                                                keyboardType =
                                                    KeyboardType.Password
                                            ),
                                        singleLine =
                                            true
                                    )
                                }

                                if (
                                    currentMode ==
                                    AuthMode.REGISTER
                                ) {
                                    OutlinedTextField(
                                        value =
                                            passwordAgain,
                                        onValueChange = {
                                            passwordAgain =
                                                it
                                        },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(),
                                        label = {
                                            Text(
                                                t("Jelszó újra")
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription =
                                                    null
                                            )
                                        },
                                        visualTransformation =
                                            if (
                                                passwordVisible
                                            ) {
                                                VisualTransformation.None
                                            } else {
                                                PasswordVisualTransformation()
                                            },
                                        keyboardOptions =
                                            KeyboardOptions(
                                                keyboardType =
                                                    KeyboardType.Password
                                            ),
                                        singleLine =
                                            true
                                    )
                                }

                                AnimatedVisibility(
                                    visible =
                                        localError != null ||
                                            state.error != null,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Text(
                                        text =
                                            localError
                                                ?: state
                                                    .error
                                                    .orEmpty(),
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .error,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall
                                    )
                                }

                                AnimatedVisibility(
                                    visible =
                                        state.message !=
                                            null,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Text(
                                        text =
                                            state.message
                                                .orEmpty(),
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodySmall
                                    )
                                }

                                Button(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(),
                                    enabled =
                                        !state.loading,
                                    onClick = {
                                        localError =
                                            null
                                        authViewModel
                                            .clearFeedback()

                                        when (
                                            currentMode
                                        ) {
                                            AuthMode.LOGIN -> {
                                                if (
                                                    email
                                                        .isBlank() ||
                                                    password
                                                        .isBlank()
                                                ) {
                                                    localError =
                                                        t("Tölts ki minden mezőt.")
                                                } else {
                                                    authViewModel
                                                        .signIn(
                                                            email,
                                                            password
                                                        )
                                                }
                                            }

                                            AuthMode.REGISTER -> {
                                                when {
                                                    name.isBlank() ||
                                                        email.isBlank() ||
                                                        password.isBlank() ||
                                                        passwordAgain.isBlank() ->
                                                        localError =
                                                            t("Tölts ki minden mezőt.")

                                                    password.length <
                                                        6 ->
                                                        localError =
                                                            t("A jelszó legalább 6 karakter legyen.")

                                                    password !=
                                                        passwordAgain ->
                                                        localError =
                                                            t("A két jelszó nem egyezik.")

                                                    else ->
                                                        authViewModel
                                                            .register(
                                                                name,
                                                                email,
                                                                password
                                                            )
                                                }
                                            }

                                            AuthMode.RESET ->
                                                authViewModel
                                                    .sendPasswordReset(
                                                        email
                                                    )
                                        }
                                    }
                                ) {
                                    if (
                                        state.loading
                                    ) {
                                        CircularProgressIndicator(
                                            modifier =
                                                Modifier
                                                    .size(
                                                        20.dp
                                                    ),
                                            strokeWidth =
                                                2.dp
                                        )
                                    } else {
                                        Text(
                                            when (
                                                currentMode
                                            ) {
                                                AuthMode.LOGIN ->
                                                    t("Belépés")

                                                AuthMode.REGISTER ->
                                                    t("Regisztráció")

                                                AuthMode.RESET ->
                                                    t("Visszaállító e-mail küldése")
                                            }
                                        )
                                    }
                                }

                                if (
                                    currentMode ==
                                    AuthMode.LOGIN
                                ) {
                                    Text(
                                        text =
                                            t("Elfelejtetted a jelszavad?"),
                                        modifier =
                                            Modifier
                                                .align(
                                                    Alignment.End
                                                )
                                                .clickable {
                                                    mode =
                                                        AuthMode.RESET
                                                    localError =
                                                        null
                                                    authViewModel
                                                        .clearFeedback()
                                                },
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium
                                    )

                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(),
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Spacer(
                                            Modifier
                                                .weight(
                                                    1f
                                                )
                                                .height(
                                                    1.dp
                                                )
                                                .background(
                                                    MaterialTheme
                                                        .colorScheme
                                                        .outlineVariant
                                                )
                                        )
                                        Text(
                                            t("  vagy  "),
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                        )
                                        Spacer(
                                            Modifier
                                                .weight(
                                                    1f
                                                )
                                                .height(
                                                    1.dp
                                                )
                                                .background(
                                                    MaterialTheme
                                                        .colorScheme
                                                        .outlineVariant
                                                )
                                        )
                                    }

                                    OutlinedButton(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(),
                                        enabled =
                                            !state.loading &&
                                                activity !=
                                                null,
                                        onClick = {
                                            activity
                                                ?.let {
                                                    authViewModel
                                                        .signInWithGoogle(
                                                            it
                                                        )
                                                }
                                        }
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(
                                                        28.dp
                                                    )
                                                    .clip(
                                                        CircleShape
                                                    )
                                                    .background(
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primaryContainer
                                                    ),
                                            contentAlignment =
                                                Alignment.Center
                                        ) {
                                            Text(
                                                "G",
                                                fontWeight =
                                                    FontWeight.Bold
                                            )
                                        }
                                        Spacer(
                                            Modifier.width(
                                                10.dp
                                            )
                                        )
                                        Text(
                                            t("Folytatás Google-lel")
                                        )
                                    }
                                }

                                TextButton(
                                    modifier =
                                        Modifier
                                            .align(
                                                Alignment.CenterHorizontally
                                            ),
                                    onClick = {
                                        localError =
                                            null
                                        authViewModel
                                            .clearFeedback()
                                        mode =
                                            when (
                                                currentMode
                                            ) {
                                                AuthMode.LOGIN ->
                                                    AuthMode.REGISTER

                                                AuthMode.REGISTER ->
                                                    AuthMode.LOGIN

                                                AuthMode.RESET ->
                                                    AuthMode.LOGIN
                                            }
                                    }
                                ) {
                                    Text(
                                        when (
                                            currentMode
                                        ) {
                                            AuthMode.LOGIN ->
                                                t("Nincs még fiókod? Regisztrálj")

                                            AuthMode.REGISTER ->
                                                t("Már van fiókod? Lépj be")

                                            AuthMode.RESET ->
                                                t("Vissza a belépéshez")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                Modifier.height(
                    16.dp
                )
            )

            Text(
                text =
                    t("A fiók a családi szinkronhoz és a több készülékes használathoz szükséges."),
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FirebaseSetupContent(
    authViewModel: AuthViewModel
) {
    Text(
        t("Firebase beállítása szükséges"),
        style =
            MaterialTheme
                .typography
                .headlineSmall,
        fontWeight =
            FontWeight.Bold
    )

    Text(
        t("A belépési rendszer már be van építve, de ehhez a buildhez még nincs Firebase Android konfiguráció."),
        style =
            MaterialTheme
                .typography
                .bodyMedium
    )

    Text(
        "1. Firebase Console → Android app\n" +
            "2. Package: " +
            BuildConfig.APPLICATION_ID +
            "\n3. google-services.json → app/\n" +
            "4. Authentication → Email/Password + Google"
    )

    if (BuildConfig.DEBUG) {
        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            onClick = {
                authViewModel
                    .continueOfflineDebug()
            }
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription =
                    null
            )
            Spacer(
                Modifier.width(
                    8.dp
                )
            )
            Text(
                t("Folytatás offline fejlesztői módban")
            )
        }
    }
}

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity ->
            this

        is ContextWrapper ->
            baseContext
                .findActivity()

        else ->
            null
    }
