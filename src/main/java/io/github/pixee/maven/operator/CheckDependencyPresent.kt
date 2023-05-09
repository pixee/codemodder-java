package io.github.pixee.maven.operator

/**
 * Guard Command Singleton use to validate required parameters
 */
val CheckDependencyPresent = object : io.github.pixee.maven.operator.AbstractSimpleCommand() {
    override fun execute(c: ProjectModel): Boolean {
        /**
         * CheckDependencyPresent requires a Dependency to be Present
         */
        if (null == c.dependency)
            throw MissingDependencyException("Dependency must be present for modify")

        return false
    }
}