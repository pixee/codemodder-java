package io.openpixee.maven.operator

import io.openpixee.maven.operator.util.Util

val SimpleDependencyManagement = object : AbstractSimpleCommand() {
    override fun execute(c: ProjectModel): Boolean {
        val lookupExpression =
            Util.buildLookupExpressionForDependencyManagement(c.dependency)

        return handleDependency(c, lookupExpression)
    }
}