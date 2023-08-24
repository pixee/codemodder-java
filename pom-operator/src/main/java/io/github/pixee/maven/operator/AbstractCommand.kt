package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.findOutIfUpgradeIsNeeded
import io.github.pixee.maven.operator.Util.selectXPathNodes
import io.github.pixee.maven.operator.Util.upgradeVersionNode
import org.dom4j.Element
import java.io.File

/**
 * Base implementation of Command - used by SimpleDependency and SimpleInsert
 */
abstract class AbstractCommand : Command {
    /**
     * Given a POM, locate its coordinates for a given dependency based on lookupExpression and figures out the upgrade
     *
     * TODO review this
     */
    protected fun handleDependency(pm: ProjectModel, lookupExpression: String): Boolean {
        val dependencyNodes = pm.pomFile.resultPom.selectXPathNodes(lookupExpression)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0].selectXPathNodes("./m:version")

            if (1 == versionNodes.size) {
                val versionNode = versionNodes[0] as Element

                var mustUpgrade = true

                if (pm.skipIfNewer) {
                    mustUpgrade = findOutIfUpgradeIsNeeded(pm, versionNode)
                }

                if (mustUpgrade) {
                    upgradeVersionNode(pm, versionNode, pm.pomFile)
                }

                return true
            }
        }

        return false
    }

    override fun execute(pm: ProjectModel): Boolean = false

    override fun postProcess(c: ProjectModel): Boolean = false

    protected fun getLocalRepositoryPath(pm: ProjectModel): File {
        val localRepositoryPath: File = when {
            pm.repositoryPath != null -> pm.repositoryPath
            System.getenv("M2_REPO") != null -> File(System.getenv("M2_REPO"))
            System.getProperty("maven.repo.local") != null -> File(System.getProperty("maven.repo.local"))
            else -> File(
                System.getProperty("user.home"),
                ".m2/repository"
            )
        }

        return localRepositoryPath
    }
}
