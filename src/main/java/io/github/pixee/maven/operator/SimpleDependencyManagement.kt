package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.buildLookupExpressionForDependencyManagement


val SimpleDependencyManagement = object : AbstractCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        val lookupExpression =
            buildLookupExpressionForDependencyManagement(pm.dependency!!)

        return handleDependency(pm, lookupExpression)
    }
}