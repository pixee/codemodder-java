package io.openpixee.maven.operator

import com.github.zafarkhaja.semver.Version
import io.openpixee.maven.operator.util.Util.selectXPathNodes
import org.apache.commons.lang3.text.StrSubstitutor
import org.dom4j.Element

abstract class AbstractSimpleCommand : Command {
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