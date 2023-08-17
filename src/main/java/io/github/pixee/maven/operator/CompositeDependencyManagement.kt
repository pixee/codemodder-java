package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.addIndentedElement
import io.github.pixee.maven.operator.Util.selectXPathNodes
import org.dom4j.Element
import java.lang.IllegalStateException

class CompositeDependencyManagement : AbstractCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        /**
         * Abort if not multi-pom
         */
        if (pm.parentPomFiles.isEmpty()) {
            return false
        }

        var result = false

        /**
         * TODO: Make it configurable / clear WHERE one should change it
         */
        val parentPomFile = pm.parentPomFiles.last()

        // add dependencyManagement

        val dependencyManagementElement =
            if (parentPomFile.resultPom.rootElement.elements("dependencyManagement").isEmpty()) {
                parentPomFile.resultPom.rootElement.addIndentedElement(
                    parentPomFile,
                    "dependencyManagement"
                )
            } else {
                parentPomFile.resultPom.rootElement.element("dependencyManagement")
            }

        val newDependencyManagementElement = modifyDependency(
            parentPomFile,
            Util.buildLookupExpressionForDependencyManagement(pm.dependency!!),
            pm,
            dependencyManagementElement,
            dependencyManagementNode = true,
        )

        if (pm.useProperties) {
            val newVersionNode =
                newDependencyManagementElement?.addIndentedElement(parentPomFile, "version")
                    ?: throw IllegalStateException("newDependencyManagementElement is missing")

            val whereToUpgradeVersionProperty = parentPomFile

            Util.upgradeVersionNode(pm, newVersionNode, whereToUpgradeVersionProperty)
        }

        // add dependency to pom - sans version
        modifyDependency(
            pm.pomFile,
            Util.buildLookupExpressionForDependency(pm.dependency!!),
            pm,
            pm.pomFile.resultPom.rootElement,
            dependencyManagementNode = false,
        )

        if (!result) {
            result = pm.pomFile.dirty
        }

        return result
    }

    private fun modifyDependency(
        pomFileToModify: POMDocument,
        lookupExpressionForDependency: String,
        c: ProjectModel,
        parentElement: Element,
        dependencyManagementNode: Boolean,
    ): Element? {
        val dependencyNodes =
            pomFileToModify.resultPom.selectXPathNodes(lookupExpressionForDependency)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0].selectXPathNodes("./m:version")

            if (1 == versionNodes.size) {
                val versionNode = versionNodes.first()

                versionNode.parent.content().remove(versionNode)

                pomFileToModify.dirty = true
            }

            return dependencyNodes[0] as Element
        } else {
            val dependenciesNode: Element =
                if (null != parentElement.element("dependencies")) {
                    parentElement.element("dependencies")
                } else {
                    parentElement.addIndentedElement(
                        pomFileToModify,
                        "dependencies"
                    )
                }

            val dependencyNode: Element =
                dependenciesNode.addIndentedElement(pomFileToModify, "dependency")

            dependencyNode.addIndentedElement(pomFileToModify, "groupId").text =
                c.dependency!!.groupId
            dependencyNode.addIndentedElement(pomFileToModify, "artifactId").text =
                c.dependency!!.artifactId

            if (dependencyManagementNode) {
                if (!c.useProperties) {
                    dependencyNode.addIndentedElement(pomFileToModify, "version").text =
                        c.dependency!!.version!!
                }
            }

            pomFileToModify.dirty = true

            return dependencyNode
        }

        return null
    }
}