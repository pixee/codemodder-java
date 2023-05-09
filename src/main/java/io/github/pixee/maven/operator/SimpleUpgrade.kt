package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.buildLookupExpressionForDependency


/**
 * Represents bumping an existing dependency/
 */
val SimpleUpgrade = object : io.github.pixee.maven.operator.AbstractSimpleCommand() {
    override fun execute(c: ProjectModel): Boolean {
        val lookupExpressionForDependency =
            buildLookupExpressionForDependency(c.dependency!!)

        return handleDependency(c, lookupExpressionForDependency)
    }
}