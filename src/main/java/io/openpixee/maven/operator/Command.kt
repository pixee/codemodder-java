package io.openpixee.maven.operator

import io.openpixee.maven.operator.util.Util.buildLookupExpressionForDependency
import io.openpixee.maven.operator.util.Util.buildLookupExpressionForDependencyManagement
import io.openpixee.maven.operator.util.Util.selectXPathNodes
import org.apache.commons.lang3.StringUtils
import org.dom4j.Element
import org.dom4j.Text
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.dom4j.tree.DefaultText
import java.io.StringReader
import java.io.StringWriter


fun interface Command {
    fun execute(c: Context): Boolean
}

val SimpleUpgrade = object : Command {
    override fun execute(c: Context): Boolean {
        val lookupExpressionForDependency = buildLookupExpressionForDependency(c.dependencyToInsert)

        val dependencyNodes = c.resultPom.selectXPathNodes(lookupExpressionForDependency)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0].selectXPathNodes("./m:version")

            if (1 == versionNodes.size) {
                versionNodes[0].text = c.dependencyToInsert.version

                return true
            }
        }

        return false
    }
}

val SimpleDependencyManagement = object : Command {
    override fun execute(c: Context): Boolean {
        val lookupExpression = buildLookupExpressionForDependencyManagement(c.dependencyToInsert)

        val dependencyNodes = c.resultPom.selectXPathNodes(lookupExpression)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0].selectXPathNodes("./m:version")

            if (1 == versionNodes.size) {
                versionNodes[0].text = c.dependencyToInsert.version

                return true
            }
        }

        return false
    }
}

val SimpleInsert = object : Command {
    override fun execute(c: Context): Boolean {
        val dependencyManagementNode =
            c.resultPom.selectXPathNodes("/m:project/m:dependencyManagement")
        val elementsToFormat : MutableList<Element> = arrayListOf()

        if (dependencyManagementNode.isEmpty()) {
            val newDependencyManagementNode =
                c.resultPom.rootElement.addElement("dependencyManagement")

            val dependenciesNode = newDependencyManagementNode.addElement("dependencies")

            val dependencyNode = appendCoordinates(dependenciesNode, c)

            val versionNode = dependencyNode.addElement("version")

            versionNode.text = c.dependencyToInsert.version

            elementsToFormat.add(newDependencyManagementNode)
        }

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

    private fun formatNode(node: Element) {
        val parent = node.parent
        val siblings = parent.content()

        val indentLevel = findIndentLevel(node)

        val clonedNode = node.clone() as Element

        val out = StringWriter()

        val outputFormat = OutputFormat.createPrettyPrint()

        val xmlWriter = XMLWriter(out, outputFormat)

        xmlWriter.setIndentLevel(Math.ceil(indentLevel.toDouble() / 2).toInt())

        xmlWriter.write(clonedNode)

        val content = out.toString()

        val newElement = SAXReader().read(StringReader(content)).rootElement.clone() as Element

        parent.remove(node)

        parent.add(DefaultText("\n" + StringUtils.repeat(" ", indentLevel)))
        parent.add(newElement)
        parent.add(DefaultText("\n" + StringUtils.repeat(" ", ((indentLevel-1) / 2))))
    }

    private fun findIndentLevel(node: Element): Int {
        val siblings = node.parent.content()
        val myIndex = siblings.indexOf(node)

        if (myIndex > 0) {
            val lastElement = siblings.subList(0, myIndex).findLast {
                (it is Text) && it.text.matches(Regex("\\n+\\s+"))
            }

            val lastElementText = lastElement?.text ?: ""

            val len = lastElementText.trimStart('\n')?.length ?: 0

            return len
        }

        return 0
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

class Chain(vararg c: Command) {
    private val commandList = ArrayList(c.toList())

    fun execute(c: Context): Boolean {
        var done = false
        val listIterator = commandList.listIterator()

        while ((!done) && listIterator.hasNext()) {
            val nextCommand = listIterator.next()

            done = nextCommand.execute(c)
        }

        return done
    }

    companion object {
        fun create() = Chain(SimpleUpgrade, SimpleDependencyManagement, SimpleInsert)
    }
}