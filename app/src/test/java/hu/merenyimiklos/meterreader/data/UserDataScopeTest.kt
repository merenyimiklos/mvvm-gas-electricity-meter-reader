package hu.merenyimiklos.meterreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDataScopeTest {
    @Test
    fun storageIdIsStableAndDoesNotLeakUidToFileName() {
        val uid = "some/fake-firebase-uid:123"
        val name = UserDataScope.storageId(uid)
        assertEquals(name, UserDataScope.storageId(uid))
        assertTrue(name.matches(Regex("[0-9a-f]{64}")))
        assertFalse(name.contains(uid))
    }

    @Test
    fun differentAccountsDoNotShareStorageNamespace() {
        assertNotEquals(
            UserDataScope.storageId("firebase-user-a"),
            UserDataScope.storageId("firebase-user-b")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyAccountIdIsRejected() {
        UserDataScope.storageId("")
    }
}
