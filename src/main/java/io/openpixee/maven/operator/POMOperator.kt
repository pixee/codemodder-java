package io.openpixee.maven.operator


/**
 * Fa&ccedil;ade for the POM Operator
 */
object POMOperator {
    /**
     * Bump a Dependency Version on a POM
     *
     * @param projectModel Project Model (Context) class
     */
    @JvmStatic
    fun modify(projectModel: ProjectModel) = Chain.createForModify().execute(projectModel)

    /**
     * Public API - Query for all the artifacts referenced inside a POM File
     *
     * @param projectModel Project Model (Context) Class
     */
    @JvmStatic
    fun queryDependency(
        projectModel: ProjectModel
    ) = POMOperator.queryDependency(projectModel, emptyList())

    /**
     * Internal Use (package-wide) - Query for all the artifacts mentioned on a POM
     *
     * @param projectModel Project Model (Context) class
     * @param commandList do not use (required for tests)
     */
    @JvmStatic
    internal fun queryDependency(
        projectModel: ProjectModel,
        commandList: List<Command>
    ): Collection<Dependency> {
        val chain = Chain.createForQuery(projectModel.queryType)

        if (commandList.isNotEmpty()) {
            chain.commandList.clear()
            chain.commandList.addAll(commandList)
        }

        chain.execute(projectModel)

        val lastCommand = chain.commandList.filterIsInstance<AbstractSimpleQueryCommand>()
            .lastOrNull { it.result != null }
            ?: return emptyList()

        return lastCommand.result!!
    }
}
