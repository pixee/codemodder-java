package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.Dependency
import io.github.pixee.maven.operator.ProjectModelFactory
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PropertyResolutionTest {
    @Test
    fun testPropertyResolutionWhenProfileIsDeactivatedForcefully() {
        val resolvedProperties = resolveWithProfiles("!test-profile")

        assertFalse("foo property must not be there", resolvedProperties.contains("foo"))
    }

    @Test
    fun testPropertyResolutionWhenProfileIsMissing() {
        val resolvedProperties = resolveWithProfiles()

        assertFalse("foo property must not be there", resolvedProperties.contains("foo"))
    }

    @Test
    fun testPropertyResolutionWhenProfileIsActivated() {
        val resolvedProperties = resolveWithProfiles("test-profile")

        assertTrue("foo property must be there", resolvedProperties.contains("foo"))
        assertEquals("foo property must be equal to 'bar'", resolvedProperties["foo"], "bar")
    }

    private fun resolveWithProfiles(vararg profilesToUse: String): Map<String, String> {
        LOGGER.debug("resolving with profiles: {}", profilesToUse)

        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.2")
        val context =
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-1.xml")!!,
            ).withDependency(dependencyToUpgrade).withActiveProfiles(*profilesToUse).build()

        LOGGER.debug("Resolved Properties: {}", context.resolvedProperties)

        return context.resolvedProperties
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)
    }
}