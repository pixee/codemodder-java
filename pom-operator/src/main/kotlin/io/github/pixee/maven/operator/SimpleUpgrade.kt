package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.buildLookupExpressionForDependency


/**
 * Represents bumping an existing dependency/
 */
val SimpleUpgrade = object : AbstractCommand() {
    override fun execute(pm: ProjectModel): Boolean {
        val lookupExpressionForDependency =
            buildLookupExpressionForDependency(pm.dependency!!)

        return handleDependency(pm, lookupExpressionForDependency)
    }
}