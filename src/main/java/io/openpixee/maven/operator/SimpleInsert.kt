package io.openpixee.maven.operator

import io.openpixee.maven.operator.Util.formatNode
import io.openpixee.maven.operator.Util.selectXPathNodes
import io.openpixee.maven.operator.Util.upgradeVersionNode
import org.dom4j.Element

/**
 * Represents a POM Upgrade Strategy by simply adding a dependency/ section (and optionally a dependencyManagement/ section as well)
 */
val SimpleInsert = object : Command {
    override fun execute(c: ProjectModel): Boolean {
        val dependencyManagementNodeList =
            c.resultPom.selectXPathNodes("/m:project/m:dependencyManagement")

        val elementsToFormat: MutableList<Element> = arrayListOf()

        val dependenciesNode = if (dependencyManagementNodeList.isEmpty()) {
            val newDependencyManagementNode =
                c.resultPom.rootElement.addElement("dependencyManagement")

            elementsToFormat.add(newDependencyManagementNode)

            newDependencyManagementNode.addElement("dependencies")
        } else {
            (dependencyManagementNodeList.first() as Element).element("dependencies").apply {
                elementsToFormat.add(this)
            }
        }

        val dependencyNode = appendCoordinates(dependenciesNode, c)

        val versionNode = dependencyNode.addElement("version")

        upgradeVersionNode(c, versionNode)

        val dependenciesNodeList = c.resultPom.selectXPathNodes("//m:project/m:dependencies")

        val rootDependencyNode: Element = if (dependenciesNodeList.isEmpty()) {
            c.resultPom.rootElement.addElement("dependencies")
        } else if (dependenciesNodeList.size == 1) {
            dependenciesNodeList[0] as Element
        } else {
            throw IllegalStateException("More than one dependencies node")
        }

        elementsToFormat.add(rootDependencyNode)

        appendCoordinates(rootDependencyNode, c)

        elementsToFormat.forEach { formatNode(it) }

        return true
    }

    /**
     * Creates the XML Elements for a given dependency
     */
    private fun appendCoordinates(
        dependenciesNode: Element,
        c: ProjectModel
    ): Element {
        val dependencyNode = dependenciesNode.addElement("dependency")

        val groupIdNode = dependencyNode.addElement("groupId")

        val dep = c.dependency!!

        groupIdNode.text = dep.groupId

        val artifactIdNode = dependencyNode.addElement("artifactId")

        artifactIdNode.text = dep.artifactId

        return dependencyNode
    }
}