package hu.merenyimiklos.meterreader.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import hu.merenyimiklos.meterreader.BuildConfig
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)

class AuthRepository(
    private val context: Context
) {
    val isConfigured: Boolean
        get() =
            BuildConfig.FIREBASE_CONFIGURED_AT_BUILD &&
                ensureFirebaseApp() != null

    fun currentUser(): AuthUser? =
        if (isConfigured) {
            FirebaseAuth
                .getInstance()
                .currentUser
                ?.toAuthUser()
        } else {
            null
        }

    suspend fun signIn(
        email: String,
        password: String
    ): AuthUser {
        requireConfigured()

        val result =
            FirebaseAuth
                .getInstance()
                .signInWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

        return requireNotNull(
            result.user
        ) {
            "Nem sikerült beolvasni a felhasználói profilt."
        }.toAuthUser()
    }

    suspend fun register(
        displayName: String,
        email: String,
        password: String
    ): AuthUser {
        requireConfigured()

        val auth =
            FirebaseAuth.getInstance()
        val result =
            auth.createUserWithEmailAndPassword(
                email.trim(),
                password
            ).await()

        val user =
            requireNotNull(
                result.user
            ) {
                "Nem sikerült létrehozni a felhasználói profilt."
            }

        val cleanName =
            displayName.trim()

        if (cleanName.isNotBlank()) {
            user.updateProfile(
                UserProfileChangeRequest
                    .Builder()
                    .setDisplayName(
                        cleanName
                    )
                    .build()
            ).await()
            user.reload().await()
        }

        return (
            auth.currentUser
                ?: user
            ).toAuthUser()
    }

    suspend fun sendPasswordReset(
        email: String
    ) {
        requireConfigured()

        FirebaseAuth
            .getInstance()
            .sendPasswordResetEmail(
                email.trim()
            )
            .await()
    }

    suspend fun signInWithGoogle(
        activity: Activity
    ): AuthUser {
        requireConfigured()

        val serverClientId =
            defaultWebClientId()
        val googleSignInOption =
            GetSignInWithGoogleOption
                .Builder(
                    serverClientId
                )
                .build()

        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(
                    googleSignInOption
                )
                .build()

        val credential =
            CredentialManager
                .create(activity)
                .getCredential(
                    context = activity,
                    request = request
                )
                .credential

        require(
            credential is CustomCredential &&
                credential.type ==
                GoogleIdTokenCredential
                    .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            "A Google nem adott vissza használható azonosítót."
        }

        val googleCredential =
            GoogleIdTokenCredential
                .createFrom(
                    credential.data
                )
        val firebaseCredential =
            GoogleAuthProvider
                .getCredential(
                    googleCredential.idToken,
                    null
                )

        val result =
            FirebaseAuth
                .getInstance()
                .signInWithCredential(
                    firebaseCredential
                )
                .await()

        return requireNotNull(
            result.user
        ) {
            "A Google-belépés sikerült, de a profil nem tölthető be."
        }.toAuthUser()
    }

    suspend fun signOut() {
        if (!isConfigured) {
            return
        }

        FirebaseAuth
            .getInstance()
            .signOut()

        runCatching {
            CredentialManager
                .create(context)
                .clearCredentialState(
                    ClearCredentialStateRequest()
                )
        }
    }

    private fun defaultWebClientId(): String {
        val resourceId =
            context.resources
                .getIdentifier(
                    "default_web_client_id",
                    "string",
                    context.packageName
                )

        require(resourceId != 0) {
            "Hiányzik a Google OAuth kliens. Engedélyezd a Google belépést Firebase-ben, majd töltsd le újra a google-services.json fájlt."
        }

        return context.getString(
            resourceId
        )
    }

    private fun requireConfigured() {
        check(isConfigured) {
            "A Firebase még nincs beállítva ehhez a buildhez."
        }
    }

    private fun ensureFirebaseApp(): FirebaseApp? =
        FirebaseApp
            .getApps(context)
            .firstOrNull()
            ?: FirebaseApp
                .initializeApp(
                    context
                )

    private fun FirebaseUser.toAuthUser() =
        AuthUser(
            uid = uid,
            email =
                email.orEmpty(),
            displayName =
                displayName
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: email
                        ?.substringBefore(
                            "@"
                        )
                        .orEmpty(),
            photoUrl =
                photoUrl
                    ?.toString()
        )
}
