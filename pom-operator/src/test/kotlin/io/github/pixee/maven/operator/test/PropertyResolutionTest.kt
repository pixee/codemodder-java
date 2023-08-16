package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.Dependency
import io.github.pixee.maven.operator.ProjectModelFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PropertyResolutionTest {
    @Test
    fun testPropertyResolutionWhenProfileIsDeactivatedForcefully() {
        val resolvedProperties = resolveWithProfiles("!test-profile")

        Assertions.assertFalse(resolvedProperties.contains("foo"), "foo property must not be there")
    }

    @Test
    fun testPropertyResolutionWhenProfileIsMissing() {
        val resolvedProperties = resolveWithProfiles()

        Assertions.assertFalse(resolvedProperties.contains("foo"), "foo property must not be there")
    }

    @Test
    fun testPropertyResolutionWhenProfileIsActivated() {
        val resolvedProperties = resolveWithProfiles("test-profile")

        assertTrue(resolvedProperties.contains("foo"), "foo property must be there")
        assertEquals(resolvedProperties["foo"], "bar", "foo property must be equal to 'bar'")
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
