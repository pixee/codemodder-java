package io.openpixee.maven.operator


/**
 * Fa&ccedil;ade for the POM Upgrader
 */
object POMOperator {
    @JvmStatic
    fun modify(projectModel: ProjectModel) = Chain.create().execute(projectModel)
}
