package io.github.pixee.maven.operator

import java.io.File

val CHECK_PARENT_DIR_COMMAND = object : AbstractQueryCommand() {
    override fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModel) =
        throw InvalidContextException()

    override fun execute(c: ProjectModel): Boolean {
        val localRepositoryPath = getLocalRepositoryPath(c)

        if (!localRepositoryPath.exists()) {
            localRepositoryPath.mkdirs()
        }

        return false
    }
}