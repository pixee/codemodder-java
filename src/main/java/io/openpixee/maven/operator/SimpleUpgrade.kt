package io.openpixee.maven.operator

import io.openpixee.maven.operator.util.Util

/**
 * Represents bumping an existing dependency/
 */
val SimpleUpgrade = object : AbstractSimpleCommand() {
    override fun execute(c: ProjectModel): Boolean {
        val lookupExpressionForDependency =
            Util.buildLookupExpressionForDependency(c.dependency)

        return handleDependency(c, lookupExpressionForDependency)
    }
}