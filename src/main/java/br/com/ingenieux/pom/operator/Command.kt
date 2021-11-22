package br.com.ingenieux.pom.operator

import br.com.ingenieux.pom.operator.util.Util.buildLookupExpressionForDependency
import br.com.ingenieux.pom.operator.util.Util.buildLookupExpressionForDependencyManagement
import br.com.ingenieux.pom.operator.util.Util.selectXPathNodes
import org.dom4j.Element


abstract class Command {
    open fun execute(c: Context): Boolean = false

}

class SimpleUpgrade : Command() {
    override fun execute(c: Context): Boolean {
        val lookupExpressionForDependency = buildLookupExpressionForDependency(c.dependencyToInsert)

        val dependencyNodes = c.resultPom.selectXPathNodes(lookupExpressionForDependency)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0]!!.selectXPathNodes("./m:version")

            if (1 == versionNodes.size) {
                versionNodes[0].text = c.dependencyToInsert.version

                return true
            }
        }

        return false
    }
}

class SimpleDependencyManagement : Command() {
    override fun execute(c: Context): Boolean {
        val lookupExpression = buildLookupExpressionForDependencyManagement(c.dependencyToInsert)

        val dependencyNodes = c.resultPom.selectXPathNodes(lookupExpression)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0]!!.selectXPathNodes("./m:version")

            if (1 == versionNodes.size) {
                versionNodes[0].text = c.dependencyToInsert.version

                return true
            }
        }

        return false
    }
}

class SimpleInsert : Command() {
    override fun execute(c: Context): Boolean {
        val dependencyManagementNode = c.resultPom.selectXPathNodes("/m:project/m:dependencyManagement")

        if (dependencyManagementNode.isEmpty()) {
            val dependencyManagementNode = c.resultPom.rootElement.addElement("dependencyManagement")

            val dependenciesNode = dependencyManagementNode.addElement("dependencies")

            val dependencyNode = appendCoordinates(dependenciesNode, c)

            val versionNode = dependencyNode.addElement("version")

            versionNode.text = c.dependencyToInsert.version
        }

        val dependenciesNodeList = c.resultPom.selectXPathNodes("//m:project/m:dependencies")

        val rootDependencyNode: Element = if (dependenciesNodeList.size == 0) {
            c.resultPom.rootElement.addElement("dependencies")
        } else if (dependenciesNodeList.size == 1){
            dependenciesNodeList[0] as Element
        } else {
            throw IllegalStateException("More than one dependencies node")
        }

        appendCoordinates(rootDependencyNode, c)

        return true
    }

    private fun appendCoordinates(
        dependenciesNode: Element,
        c: Context
    ): Element {
        val dependencyNode = dependenciesNode.addElement("dependency")

        val groupIdNode = dependencyNode.addElement("groupId")

        groupIdNode.text = c.dependencyToInsert.groupId

        val artifactIdNode = dependencyNode.addElement("artifactId")

        artifactIdNode.text = c.dependencyToInsert.artifactId
        return dependencyNode
    }
}

class Chain(c: List<Command>) : Command() {
    private val commandList = ArrayList(c.toList())

    override fun execute(c: Context): Boolean {
        var done = false
        val listIterator = commandList.listIterator()

        while ((!done) && listIterator.hasNext()) {
            val nextCommand = listIterator.next()

            done = nextCommand.execute(c)
        }

        return done
    }

    companion object {
        fun create() = Chain(listOf(SimpleUpgrade(), SimpleDependencyManagement(), SimpleInsert()))
    }
}