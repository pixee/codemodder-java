package io.openpixee.maven.operator


object POMOperator {
    @JvmStatic
    fun modify(projectModel: ProjectModel) = Chain.create().execute(projectModel)
}
