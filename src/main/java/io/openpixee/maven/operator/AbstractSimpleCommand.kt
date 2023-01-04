package io.openpixee.maven.operator

import io.openpixee.maven.operator.Util.findOutIfUpgradeIsNeeded
import io.openpixee.maven.operator.Util.selectXPathNodes
import io.openpixee.maven.operator.Util.upgradeVersionNode
import org.dom4j.Element

/**
 * Base implementation of Command - used by SimpleDependency and SimpleInsert
 */
abstract class AbstractSimpleCommand : Command {
    /**
     * Given a POM, locate its coordinates for a given dependency based on lookupExpression and figures out the upgrade
     */
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
                    upgradeVersionNode(c, versionNode)
                }

                return true
            }
        }

        return false
    }

}