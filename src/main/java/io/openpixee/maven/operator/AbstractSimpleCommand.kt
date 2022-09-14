package io.openpixee.maven.operator

import com.github.zafarkhaja.semver.Version
import io.openpixee.maven.operator.util.Util.selectXPathNodes

abstract class AbstractSimpleCommand : Command {
    protected fun handleDependency(c: ProjectModel, lookupExpression: String): Boolean {
        val dependencyNodes = c.resultPom.selectXPathNodes(lookupExpression)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0].selectXPathNodes("./m:version")

            if (1 == versionNodes.size) {
                var mustUpgrade = true

                if (c.skipIfNewer) {
                    // TODO: Handle Properties
                    val currentVersion = Version.valueOf(versionNodes[0].text)
                    val newVersion = Version.valueOf(c.dependency.version)

                    val versionsAreIncreasing = newVersion.greaterThan(currentVersion)

                    mustUpgrade = versionsAreIncreasing
                }

                if (mustUpgrade) {
                    versionNodes[0].text = c.dependency.version
                }

                return true
            }
        }

        return false
    }
}