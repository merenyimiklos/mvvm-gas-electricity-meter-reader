package hu.merenyimiklos.meterreader.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun stableReleaseIsNewerThanBetaOfSameVersion() {
        assertTrue(
            VersionComparator.isNewer(
                current =
                    "0.6.0-beta.12",
                latest =
                    "0.6.0"
            )
        )
    }

    @Test
    fun newerPatchIsDetected() {
        assertTrue(
            VersionComparator.isNewer(
                current = "0.5.9",
                latest = "0.6.0"
            )
        )
    }

    @Test
    fun firstPublicReleaseIsNewerThanLegacyPreOnePointZero() {
        assertTrue(
            VersionComparator.isNewer(
                current = "0.13.0",
                latest = "1.0.0"
            )
        )
        assertTrue(
            VersionComparator.isNewer(
                current = "0.14.0",
                latest = "1.0.0"
            )
        )
        assertFalse(
            VersionComparator.isNewer(
                current = "1.0.0",
                latest = "0.14.0"
            )
        )
    }

    @Test
    fun olderReleaseIsNotUpdate() {
        assertFalse(
            VersionComparator.isNewer(
                current = "0.6.0",
                latest = "0.5.9"
            )
        )
    }
}
