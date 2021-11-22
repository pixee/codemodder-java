package br.com.ingenieux.pom.operator.util

import br.com.ingenieux.pom.operator.Dependency
import org.dom4j.Node
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

    fun Node.selectXPathNodes(expression: String) =
        createXPathExpression(expression).selectNodes(this) as List<Node>

    fun createXPathExpression(expression: String): XPath {
        val xpath = Dom4jXPath(expression)

        xpath.namespaceContext = namespaceContext

        return xpath
    }

    internal val namespaceContext = SimpleNamespaceContext(
        mapOf(
            "m" to "http://maven.apache.org/POM/4.0.0"
        )
    )

}