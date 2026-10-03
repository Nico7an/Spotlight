package fr.nico7an.spotlight.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {
    @Test
    fun `numeric comparison, not lexicographic`() = assertTrue(Versions.compare("1.0.10", "1.0.9") > 0)

    @Test
    fun `equal versions`() = assertEquals(0, Versions.compare("v1.0.6", "1.0.6"))

    @Test
    fun `dev build is older than any release`() = assertTrue(Versions.compare("1.0.1", "1.0.0-dev") > 0)

    @Test
    fun `older release is not an update`() = assertTrue(Versions.compare("1.0.5", "1.0.6") < 0)
}
