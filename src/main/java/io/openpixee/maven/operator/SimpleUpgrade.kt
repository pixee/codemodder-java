package io.openpixee.maven.operator

import io.openpixee.maven.operator.Util.buildLookupExpressionForDependency


/**
 * Represents bumping an existing dependency/
 */
val SimpleUpgrade = object : AbstractSimpleCommand() {
    override fun execute(c: ProjectModel): Boolean {
        val lookupExpressionForDependency =
            buildLookupExpressionForDependency(c.dependency!!)

        return handleDependency(c, lookupExpressionForDependency)
    }
}