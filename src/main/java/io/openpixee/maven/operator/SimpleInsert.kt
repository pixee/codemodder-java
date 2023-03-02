package io.openpixee.maven.operator

import io.openpixee.maven.operator.Util.addIndentedElement
import io.openpixee.maven.operator.Util.findIndentLevel
import io.openpixee.maven.operator.Util.selectXPathNodes
import io.openpixee.maven.operator.Util.upgradeVersionNode
import org.apache.commons.lang3.StringUtils
import org.dom4j.Element
import org.dom4j.Text
import org.dom4j.tree.DefaultText

/**
 * Represents a POM Upgrade Strategy by simply adding a dependency/ section (and optionally a dependencyManagement/ section as well)
 */
val SimpleInsert = object : Command {
    override fun execute(c: ProjectModel): Boolean {
        val dependencyManagementNodeList =
            c.resultPom.selectXPathNodes("/m:project/m:dependencyManagement")

        val dependenciesNode = if (dependencyManagementNodeList.isEmpty()) {
            val newDependencyManagementNode =
                c.resultPom.rootElement.addIndentedElement(c, "dependencyManagement")

            val dependencyManagementNode =
                newDependencyManagementNode.addIndentedElement(c, "dependencies")

            dependencyManagementNode
        } else {
            (dependencyManagementNodeList.first() as Element).element("dependencies")
        }

        val dependencyNode = appendCoordinates(dependenciesNode, c)

        val versionNode = dependencyNode.addIndentedElement(c, "version")

        upgradeVersionNode(c, versionNode)

        val dependenciesNodeList = c.resultPom.selectXPathNodes("//m:project/m:dependencies")

        val rootDependencyNode: Element = if (dependenciesNodeList.isEmpty()) {
            c.resultPom.rootElement.addIndentedElement(c, "dependencies")
        } else if (dependenciesNodeList.size == 1) {
            dependenciesNodeList[0] as Element
        } else {
            throw IllegalStateException("More than one dependencies node")
        }

        appendCoordinates(rootDependencyNode, c)

        return true
    }

    /**
     * Creates the XML Elements for a given dependency
     */
    private fun appendCoordinates(
        dependenciesNode: Element,
        c: ProjectModel
    ): Element {
        val dependencyNode = dependenciesNode.addIndentedElement(c, "dependency")

        val groupIdNode = dependencyNode.addIndentedElement(c, "groupId")

        val dep = c.dependency!!

        groupIdNode.text = dep.groupId

        val artifactIdNode = dependencyNode.addIndentedElement(c, "artifactId")

        artifactIdNode.text = dep.artifactId

        return dependencyNode
    }

    override fun postProcess(c: ProjectModel): Boolean = false
}

