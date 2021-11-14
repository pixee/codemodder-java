package br.com.ingenieux.pom.operator

import org.apache.commons.lang3.builder.EqualsBuilder
import org.dom4j.Document
import java.lang.IllegalStateException

data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String? = null,
    val classifier: String? = null,
    val packaging: String? = "jar"
) {
    fun matchesOther(dep: Dependency): Boolean {
        val equalsBuilder = EqualsBuilder().append(this.groupId, dep.groupId)
            .append(this.artifactId, dep.artifactId)

        if (null != classifier) {
            equalsBuilder.append(this.classifier, dep.classifier)
        }

        if (null != packaging) {
            equalsBuilder.append(this.packaging, dep.packaging)
        }

        return equalsBuilder.isEquals
    }
}

object POMOperator {
    fun upgradePom(pom: Document, dependencyToUpgrade: Dependency): Document {
        val doc = pom.clone() as Document

        if (simpleUpgrade(doc, dependencyToUpgrade)) {
            return doc
        }

        if (simpleDependencyManagementUpgrade(doc, dependencyToUpgrade)) {
            return doc
        }

        throw IllegalStateException("Unable to upgrade dependency")
    }

    private fun simpleUpgrade(doc: Document, dependencyToUpgrade: Dependency): Boolean {
        val lookupExpressionForDependency = buildLookupExpressionForDependency(dependencyToUpgrade)

        return upgradeDependency(doc, lookupExpressionForDependency, dependencyToUpgrade)
    }

    private fun simpleDependencyManagementUpgrade(doc: Document, dependencyToUpgrade: Dependency): Boolean {
        val lookupExpressionForDependency = buildLookupExpressionForDependencyManagement(dependencyToUpgrade)

        return upgradeDependency(doc, lookupExpressionForDependency, dependencyToUpgrade)
    }

    private fun upgradeDependency(
        doc: Document,
        lookupExpression: String,
        dep: Dependency
    ): Boolean {
        val dependencyNodes = doc.selectNodes(lookupExpression)

        if (1 == dependencyNodes.size) {
            val versionNodes = dependencyNodes[0]!!.selectNodes("./*[local-name()='version']")

            if (1 == versionNodes.size) {
                versionNodes[0].text = dep.version

                return true
            }
        }

        return false
    }

    private fun buildLookupExpressionForDependency(dependency: Dependency) =
        "/*[local-name()='project']" +
                "/*[local-name()='dependencies']" +
                "/*[local-name()='dependency']" +
                /* */ "[./*[local-name()='groupId'][text()='${dependency.groupId}'] and " +
                /*  */ "./*[local-name()='artifactId'][text()='${dependency.artifactId}']" +
                "]"
    private fun buildLookupExpressionForDependencyManagement(dependency: Dependency) =
        "/*[local-name()='project']" +
                "/*[local-name()='dependencyManagement']" +
                "/*[local-name()='dependencies']" +
                "/*[local-name()='dependency']" +
                /* */ "[./*[local-name()='groupId'][text()='${dependency.groupId}'] and " +
                /*  */ "./*[local-name()='artifactId'][text()='${dependency.artifactId}']" +
                "]"

//    // "/*[local-name()='project']/*[local-name()='dependencies']/*[local-name()='dependency']"
//    private fun selectDependencyNodes(doc: Document, expr: String): List<Node>
//    {
//        val expandedWithNamespaces = expandNS(expr)
//        val result = doc.selectNodes(expandedWithNamespaces)
//        return result
//    }
//
//    private fun expandNS(xpathExpr: String): String {
//        val exprBuilder = StringBuilder()
//
//        for (element: String in xpathExpr.substring(1).split("/")) {
//            val whatToAppend = element.replace(Regex("""^(\w+)(.*)""")) { matchResult ->
//                "/*[local-name()='${matchResult.groups[1]!!.value}']${matchResult.groups[2]!!.value}"
//            }
//            exprBuilder.append(whatToAppend)
//        }
//
//        return exprBuilder.toString()
//    }
}