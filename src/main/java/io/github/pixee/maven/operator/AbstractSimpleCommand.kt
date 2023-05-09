package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.findOutIfUpgradeIsNeeded
import io.github.pixee.maven.operator.Util.selectXPathNodes
import io.github.pixee.maven.operator.Util.upgradeVersionNode
import org.dom4j.Element

/**
 * Base implementation of Command - used by SimpleDependency and SimpleInsert
 */
abstract class AbstractSimpleCommand : io.github.pixee.maven.operator.Command {
    /**
     * Given a POM, locate its coordinates for a given dependency based on lookupExpression and figures out the upgrade
     */
    protected fun handleDependency(c: io.github.pixee.maven.operator.ProjectModel, lookupExpression: String): Boolean {
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

    override fun execute(c: io.github.pixee.maven.operator.ProjectModel): Boolean = false

    override fun postProcess(c: io.github.pixee.maven.operator.ProjectModel): Boolean = false
}