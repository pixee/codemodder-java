package br.com.ingenieux.pom.operator

import br.com.ingenieux.pom.operator.util.Util.buildLookupExpressionForDependency
import br.com.ingenieux.pom.operator.util.Util.buildLookupExpressionForDependencyManagement
import br.com.ingenieux.pom.operator.util.Util.selectXPathNodes
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import java.io.IOException
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

        if (dependencyManagementNode.isEmpty()) {
            val newDependencyManagementNode =
                c.resultPom.rootElement.addElement("dependencyManagement")

            val dependenciesNode = newDependencyManagementNode.addElement("dependencies")

            val dependencyNode = appendCoordinates(dependenciesNode, c)

            val versionNode = dependencyNode.addElement("version")

            versionNode.text = c.dependencyToInsert.version

            formatNode(newDependencyManagementNode, true)
        }

        val dependenciesNodeList = c.resultPom.selectXPathNodes("//m:project/m:dependencies")

        val rootDependencyNode: Element = if (dependenciesNodeList.isEmpty()) {
            c.resultPom.rootElement.addElement("dependencies")
        } else if (dependenciesNodeList.size == 1) {
            dependenciesNodeList[0] as Element
        } else {
            throw IllegalStateException("More than one dependencies node")
        }

        appendCoordinates(rootDependencyNode, c)

        return true
    }

    private fun formatNode(node: Element, parent: Boolean = false) {
        // TODO: Guess Indent Level
        // TODO: Guess DOS / Windows Line Terminations
        // TODO: tweak it a bit further (am I the first sibling or not? if so, how to leverage that?)

        val myIndex = node.parent.indexOf(node)

        val contentList = (node.parent as Element).content() as MutableList

        val previousElement = contentList.filterIndexed { index, node ->
            node is Element && index < myIndex
        }.last()

        val previousElementIndex = node.parent.indexOf(previousElement)

        val lastElegibleWhitespace = contentList.filterIndexed { index, node ->
            node is Text && index < myIndex && node.text.isBlank() && node.text.isNotEmpty() && node.text.length > 1
        }.last()!!.text

        var leadingWhitespace = lastElegibleWhitespace.replace(Regex("^[\\r\\n]{2,}"), "\n")

        // Deletes any matching whitespaced text between the two elements

        val elementsToDelete = contentList.filterIndexed { index, node ->
            node is Text && index < myIndex && index > previousElementIndex && node.text.isBlank()
        }.toList()

        contentList.removeAll(elementsToDelete)

        val newTextNode = DefaultText(leadingWhitespace)

        contentList.add(-1 + myIndex, newTextNode)

        //

        if (!parent) {
            val siblingNode = findSiblingNode(node)

            val siblingParent = siblingNode.parent
            val siblingIndex = siblingParent.content().indexOf(siblingNode)

            siblingParent.content().add(1 + siblingIndex, DefaultText("\n  "))
        }

        // one more thing: do I have any child elements? lets apply padding to them

        var itNode = node

        while (itNode.hasContent() && itNode.content().any { it is Element }) {
            leadingWhitespace += "  "

            val childElements = itNode.content().filter { it is Element }.map { it as Element }
            val firstChild = childElements.first()
            itNode.content().add(0, DefaultText(leadingWhitespace))

            // and tell'em to do the same

            if (childElements.size > 1) {
                childElements.subList(1, childElements.size).forEach { formatNode(it, false) }
            } else if (1 == childElements.size) {
                itNode = childElements.first()
                continue
            }

            break
        }

        // Find the elements after the ones we added with continuous other elements and indent as well
    }

    private fun findSiblingNode(node: Element): Element {
        val parentNode: Element = (node.parent as Element)

        val siblings = parentNode.elements()

        val siblingNode = siblings[-1 + siblings.indexOf(node)]

        if (siblingNode.elements().isNotEmpty()) {
            return siblingNode.elements().last()
        }

        return siblingNode
    }
    */

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