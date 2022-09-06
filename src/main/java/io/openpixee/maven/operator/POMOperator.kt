package io.openpixee.maven.operator


object POMOperator {
    @JvmStatic
    fun upgradePom(projectModel: ProjectModel) = Chain.create().execute(projectModel)
}
