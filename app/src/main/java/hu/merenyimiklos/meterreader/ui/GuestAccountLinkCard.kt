package hu.merenyimiklos.meterreader.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.merenyimiklos.meterreader.auth.AuthViewModel

/**
 * A guest may upgrade the *same* UID. Never sign into another account directly:
 * that would switch the active Room/DataStore scope and hide guest readings.
 */
@Composable
internal fun GuestAccountLinkCard(authViewModel: AuthViewModel) {
    val state by authViewModel.state.collectAsStateWithLifecycle()
    if (state.user?.isAnonymous != true) return

    val linking by authViewModel.linkingAccount.collectAsStateWithLifecycle()
    val linkError by authViewModel.linkError.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findLinkActivity()
    var emailFieldsVisible by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            t("Vendégfiók"),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            t("A mérőállásaid most ehhez az ideiglenes fiókhoz kötődnek. " +
                "Kapcsold össze Google-lel vagy e-maillel, hogy kijelentkezés után is vissza tudj lépni. " +
                "Az összekapcsolás nem törli a méréseidet."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = !linking && activity != null,
            onClick = {
                activity?.let { authViewModel.linkGuestWithGoogle(it) }
            }
        ) {
            Text(t("Összekapcsolás Google-fiókkal"))
        }

        if (!emailFieldsVisible) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !linking,
                onClick = {
                    authViewModel.clearLinkError()
                    emailFieldsVisible = true
                }
            ) {
                Text(t("Összekapcsolás új e-mailes fiókkal"))
            }
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !linking,
                label = { Text(t("Név")) }
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !linking,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                label = { Text(t("E-mail cím")) }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !linking,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                label = { Text(t("Jelszó (legalább 6 karakter)")) }
            )
            Button(
                onClick = { authViewModel.linkGuestWithEmail(name, email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !linking && email.isNotBlank() && password.length >= 6
            ) {
                Text(t("Vendégfiók véglegesítése"))
            }
            TextButton(
                enabled = !linking,
                onClick = {
                    emailFieldsVisible = false
                    password = ""
                    authViewModel.clearLinkError()
                }
            ) {
                Text(t("Mégse"))
            }
        }
        if (linking) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        if (!linkError.isNullOrBlank()) {
            Text(
                authFeedbackText(linkError.orEmpty()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            t("Meglévő fiókba nem olvasztjuk automatikusan a vendégadatokat. " +
                "Ha már van fiókod, előbb készíts JSON mentést, majd azzal vidd át a méréseket."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun Context.findLinkActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findLinkActivity()
    else -> null
}
