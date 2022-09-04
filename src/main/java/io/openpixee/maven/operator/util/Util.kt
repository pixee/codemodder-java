package io.openpixee.maven.operator.util

import io.openpixee.maven.operator.Dependency
import org.dom4j.*
import org.dom4j.tree.DefaultElement
import org.jaxen.SimpleNamespaceContext
import org.jaxen.XPath
import org.jaxen.dom4j.Dom4jXPath

object Util {
    fun buildLookupExpressionForDependency(dependency: Dependency): String =
        "/m:project" +
                "/m:dependencies" +
                "/m:dependency" +
                /* */ "[./m:groupId[text()='${dependency.groupId}'] and " +
                /*  */ "./m:artifactId[text()='${dependency.artifactId}']" +
                "]"

    fun buildLookupExpressionForDependencyManagement(dependency: Dependency): String =
        "/m:project" +
                "/m:dependencyManagement" +
                "/m:dependencies" +
                "/m:dependency" +
                /* */ "[./m:groupId[text()='${dependency.groupId}'] and " +
                /*  */ "./m:artifactId[text()='${dependency.artifactId}']" +
                "]"

    @Suppress("UNCHECKED_CAST")
    fun Node.selectXPathNodes(expression: String) =
        createXPathExpression(expression).selectNodes(this)!! as List<Node>

    private fun createXPathExpression(expression: String): XPath {
        val xpath = Dom4jXPath(expression)

        xpath.namespaceContext = namespaceContext

        return xpath
    }

    private val namespaceContext = SimpleNamespaceContext(
        mapOf(
            "m" to "http://maven.apache.org/POM/4.0.0"
        )
    )

    val NAMESPACE_CLEANER = object : VisitorSupport() {
        override fun visit(document: Document) {
            (document.getRootElement() as DefaultElement)
                .setNamespace(Namespace.NO_NAMESPACE)
            document.getRootElement().additionalNamespaces().clear()
        }

        override fun visit(namespace: Namespace) {
            namespace.detach()
        }

        override fun visit(node: Attribute) {
            if (node.toString().contains("xmlns")
                || node.toString().contains("xsi:")
            ) {
                node.detach()
            }
        }

        override fun visit(node: Element) {
            if (node is DefaultElement) {
                node.setNamespace(Namespace.NO_NAMESPACE)
            }
        }
    }

}