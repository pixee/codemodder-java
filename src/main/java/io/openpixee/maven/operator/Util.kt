@file:Suppress("DEPRECATION")

package io.openpixee.maven.operator

import com.github.zafarkhaja.semver.Version
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.lang3.text.StrSubstitutor
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.Text
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.dom4j.tree.DefaultText
import org.jaxen.SimpleNamespaceContext
import org.jaxen.XPath
import org.jaxen.dom4j.Dom4jXPath
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import kotlin.math.ceil

/**
 * Common Utilities
 */
object Util {
    /**
     * Formats a XML Element Node
     */
    internal fun formatNode(node: Element) {
        val parent = node.parent
        //val siblings = parent.content()

        val indentLevel = findIndentLevel(node)

        val clonedNode = node.clone() as Element

        val out = StringWriter()

        val outputFormat = OutputFormat.createPrettyPrint()

        val xmlWriter = XMLWriter(out, outputFormat)

        xmlWriter.setIndentLevel(ceil(indentLevel.toDouble() / 2).toInt())

        xmlWriter.write(clonedNode)

        val content = out.toString()

        val newElement = SAXReader().read(StringReader(content)).rootElement.clone() as Element

        parent.remove(node)

        parent.add(DefaultText("\n" + StringUtils.repeat(" ", indentLevel)))
        parent.add(newElement)
        parent.add(DefaultText("\n" + StringUtils.repeat(" ", ((indentLevel - 1) / 2))))
    }

    /**
     * Guesses the current indent level of the nearest nodes
     */
    internal fun findIndentLevel(node: Element): Int {
        val siblings = node.parent.content()
        val myIndex = siblings.indexOf(node)

        if (myIndex > 0) {
            val lastElement = siblings.subList(0, myIndex).findLast {
                (it is Text) && it.text.matches(Regex("\\n+\\s+"))
            }

            val lastElementText = lastElement?.text ?: ""

            return lastElementText.trimStart('\n').length
        }

        return 0
    }

    /**
     * Represents a Property Reference - as a regex
     */
    internal val PROPERTY_REFERENCE_REGEX = Regex("^\\\$\\{(.*)}$")

    /**
     * Upserts a given property
     */
    internal fun upgradeProperty(c: ProjectModel, propertyName: String) {
        if (null == c.resultPom.rootElement.element("properties")) {
            val propertyElement = c.resultPom.rootElement.addElement("properties")

            formatNode(propertyElement)
        }

        val parentPropertyElement = c.resultPom.rootElement.element("properties")

        if (null == parentPropertyElement.element(propertyName)) {
            val newElement = parentPropertyElement.addElement(propertyName)

            formatNode(newElement)
        } else {
            if (!c.overrideIfAlreadyExists) {
                val propertyReferenceRE = Regex.fromLiteral("\${$propertyName}")

                val numberOfAllCurrentMatches = propertyReferenceRE.findAll(c.pomDocument.asXML()).toList().size

                if (numberOfAllCurrentMatches > 1) {
                    throw IllegalStateException("Property $propertyName is already defined - and used more than once.")
                }
            }
        }

        val propertyElement = parentPropertyElement.element(propertyName)

        propertyElement.text = c.dependency!!.version

        formatNode(propertyElement)
    }

    /**
     * Creates a property Name
     */
    internal fun propertyName(c: ProjectModel, versionNode: Element): String {
        val version = versionNode.textTrim

        if (PROPERTY_REFERENCE_REGEX.matches(version)) {
            val match = PROPERTY_REFERENCE_REGEX.find(version)

            val firstMatch = match!!.groups[1]!!

            return firstMatch.value
        }

        return "versions." + c.dependency!!.artifactId
    }

    /**
     * Identifies if an upgrade is needed
     */
    internal fun findOutIfUpgradeIsNeeded(c: ProjectModel, versionNode: Element): Boolean {
        val currentVersionNodeText = resolveVersion(c, versionNode.text!!)

        val currentVersion = Version.valueOf(currentVersionNodeText)
        val newVersion = Version.valueOf(c.dependency!!.version)

        @Suppress("UnnecessaryVariable") val versionsAreIncreasing = newVersion.greaterThan(currentVersion)

        return versionsAreIncreasing
    }

    internal fun resolveVersion(c: ProjectModel, versionText: String): String =
        if (PROPERTY_REFERENCE_REGEX.matches(versionText)) {
            @Suppress("DEPRECATION")
            StrSubstitutor(c.resolvedProperties).replace(versionText)
        } else {
            versionText
        }

    /**
     * Escapes a Property Name
     */
    internal fun escapedPropertyName(propertyName: String): String =
        "\${$propertyName}"

    /**
     * Given a Version Node, upgrades a resulting POM
     */
    internal fun upgradeVersionNode(c: ProjectModel, versionNode: Element) {
        if (c.useProperties) {
            val propertyName = propertyName(c, versionNode)

            // define property
            upgradeProperty(c, propertyName)

            versionNode.text = escapedPropertyName(propertyName)
        } else {
            versionNode.text = c.dependency!!.version
        }
    }

    /**
     * Builds a Lookup Expression String for a given dependency
     *
     * @param dependency Dependency
     */
    fun buildLookupExpressionForDependency(dependency: Dependency): String =
        "/m:project" +
                "/m:dependencies" +
                "/m:dependency" +
                /* */ "[./m:groupId[text()='${dependency.groupId}'] and " +
                /*  */ "./m:artifactId[text()='${dependency.artifactId}']" +
                "]"

    /**
     * Builds a Lookup Expression String for a given dependency, but under the &gt;dependencyManagement&gt; section
     *
     * @param dependency Dependency
     */
    fun buildLookupExpressionForDependencyManagement(dependency: Dependency): String =
        "/m:project" +
                "/m:dependencyManagement" +
                "/m:dependencies" +
                "/m:dependency" +
                /* */ "[./m:groupId[text()='${dependency.groupId}'] and " +
                /*  */ "./m:artifactId[text()='${dependency.artifactId}']" +
                "]"

    /**
     * Extension Function to Select the XPath Nodes
     *
     * @param expression expression to use
     */
    @Suppress("UNCHECKED_CAST")
    fun Node.selectXPathNodes(expression: String) =
        createXPathExpression(expression).selectNodes(this)!! as List<Node>

    /**
     * Creates a XPath Expression from a given expression string
     *
     * @param expression expression to create xpath from
     */
    private fun createXPathExpression(expression: String): XPath {
        val xpath = Dom4jXPath(expression)

        xpath.namespaceContext = namespaceContext

        return xpath
    }

    /**
     * Hard-Coded POM Namespace Map
     */
    private val namespaceContext = SimpleNamespaceContext(
        mapOf(
            "m" to "http://maven.apache.org/POM/4.0.0"
        )
    )


    internal fun which(path: String): File? {
        val nativeExecutables: List<String> = if (SystemUtils.IS_OS_WINDOWS) {
            listOf("", ".exe", ".bat", ".cmd").map { path + it }.toList()
        } else {
            listOf(path)
        }

        val pathContentString = System.getenv("PATH")

        val pathElements = pathContentString.split(File.pathSeparatorChar)

        val possiblePaths = nativeExecutables.flatMap { executable ->
            pathElements.map { pathElement ->
                File(File(pathElement), executable)
            }
        }

        val isCliCallable: (File) -> Boolean = if (SystemUtils.IS_OS_WINDOWS) { it ->
            it.exists() && it.isFile
        } else { it ->
            it.exists() && it.isFile && it.canExecute()
        }

        val result = possiblePaths.findLast(isCliCallable)

        if (null == result) {
            AbstractSimpleQueryCommand.LOGGER.warn(
                "Unable to find mvn executable (execs: {}, path: {})",
                nativeExecutables.joinToString("/"),
                pathContentString
            )
        }

        return result
    }

}
