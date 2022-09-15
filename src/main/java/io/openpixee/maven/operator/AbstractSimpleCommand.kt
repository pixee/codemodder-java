package io.openpixee.maven.operator

import com.github.zafarkhaja.semver.Version
import io.openpixee.maven.operator.util.Util.selectXPathNodes
import org.apache.commons.lang3.text.StrSubstitutor
import org.dom4j.Element

abstract class AbstractSimpleCommand : Command {
    companion object {
        val PROPERTY_REFERENCE_REGEX = Regex("^\\\$\\{(.*)}$")
    }

    protected fun handleDependency(c: ProjectModel, lookupExpression: String): Boolean {
        val dependencyNodes = c.resultPom.selectXPathNodes(lookupExpression)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0].selectXPathNodes("./m:version")

            if (1 == versionNodes.size) {
                val versionNode = versionNodes[0] as Element

                var mustUpgrade = true

                if (c.skipIfNewer) {
                    mustUpgrade = findOutIfUpgradeIsNeeded(c, versionNode)
                }

                if (mustUpgrade) {
                    if (c.useProperties) {
                        val propertyName = propertyName(c, versionNode)

                        // define property
                        upgradeProperty(c, propertyName)

                        versionNodes[0].text = "\${$propertyName}"
                    } else {
                        versionNodes[0].text = c.dependency.version
                    }
                }

                return true
            }
        }

        return false
    }

    private fun upgradeProperty(c: ProjectModel, propertyName: String) {
        // TODO: Handle Profiles

        val propertyElement = c.resultPom.rootElement.element("properties")

        if (null == propertyElement.element(propertyName)) {
            val newElement = propertyElement.addElement(propertyName)

            formatNode(newElement)
        }

        propertyElement.element(propertyName).text = c.dependency.version
    }

    private fun propertyName(c: ProjectModel, versionNode: Element): String {
        val version = versionNode.textTrim

        if (PROPERTY_REFERENCE_REGEX.matches(version)) {
            val match = PROPERTY_REFERENCE_REGEX.find(version)

            val firstMatch = match!!.groups[1]!!

            return firstMatch.value
        }

        // TODO: Escaping

        return c.dependency.artifactId + ".version"
    }

    private fun findOutIfUpgradeIsNeeded(c: ProjectModel, versionNode: Element): Boolean {
        val currentVersionNodeText = resolveVersion(c, versionNode.text!!)

        val currentVersion = Version.valueOf(currentVersionNodeText)
        val newVersion = Version.valueOf(c.dependency.version)

        val versionsAreIncreasing = newVersion.greaterThan(currentVersion)

        return versionsAreIncreasing
    }

    private fun resolveVersion(c: ProjectModel, versionText: String): String =
        if (versionText.contains("\${")) {
            @Suppress("DEPRECATION")
            StrSubstitutor(c.resolvedProperties).replace(versionText)
        } else {
            versionText
        }

}