package io.openpixee.maven.operator

/**
 * Represents a Command in a Chain of Responsibility Pattern
 */
interface Command {
    /**
     * Given a context, performs an operation
     *
     * @param c Context (Project Model) to use
     * @return true if the execution was successful *AND* the chain must end
     */
    fun execute(c: ProjectModel): Boolean

    /**
     * Post Processing, implementing a Filter Pattern
     */
    fun postProcess(c: ProjectModel): Boolean
}