package io.openpixee.maven.operator.util

import io.openpixee.maven.operator.Dependency
import org.dom4j.Node
import org.jaxen.SimpleNamespaceContext
import org.jaxen.XPath
import org.jaxen.dom4j.Dom4jXPath

/**
 * Common Utilities
 */
object Util {
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
}