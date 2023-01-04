package io.openpixee.maven.operator

import io.openpixee.maven.operator.Util.buildLookupExpressionForDependencyManagement


val SimpleDependencyManagement = object : AbstractSimpleCommand() {
    override fun execute(c: ProjectModel): Boolean {
        val lookupExpression =
            buildLookupExpressionForDependencyManagement(c.dependency!!)

        return handleDependency(c, lookupExpression)
    }
}