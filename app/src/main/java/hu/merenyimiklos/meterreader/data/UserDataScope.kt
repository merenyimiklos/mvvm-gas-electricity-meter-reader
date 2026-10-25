package hu.merenyimiklos.meterreader.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest

/**
 * A local storage namespace is tied to a Firebase UID, not to an email address.
 * The previous unscoped files are preserved solely for an explicit legacy import.
 */
object UserDataScope {
    const val DEBUG_GUEST = "debug-guest"

    fun signedInUid(context: Context): String? =
        runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                null
            } else {
                FirebaseAuth.getInstance().currentUser?.uid
            }
        }.getOrNull()

    fun storageId(uid: String): String {
        require(uid.isNotBlank()) { "A felhasználói azonosító nem lehet üres." }
        return MessageDigest.getInstance("SHA-256")
            .digest(uid.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
