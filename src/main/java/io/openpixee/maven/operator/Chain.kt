package io.openpixee.maven.operator

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Implements a Chain of Responsibility Pattern
 *
 * @constructor commands: Commands to Use
 */
class Chain(vararg commands: Command) {
    /**
     * Internal ArrayList of the Commands
     */
    internal val commandList : MutableList<Command> = ArrayList(commands.toList())

    /**
     * Executes the Commands in the Chain of Responsibility
     *
     * @param c ProjectModel (context)
     * @return Boolean if successful
     */
    fun execute(c: ProjectModel): Boolean {
        var done = false
        val listIterator = commandList.listIterator()

        while ((!done) && listIterator.hasNext()) {
            val nextCommand = listIterator.next()

            done = nextCommand.execute(c)
        }

        return done
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(Chain::class.java)

        /**
         * Some classes won't have all available dependencies on the classpath during runtime
         * for this reason we'll use <pre>Class.forName</pre> and report issues creating
         */
        val AVAILABLE_QUERY_COMMANDS = listOf(
            QueryType.SAFE to "QueryByResolver",
            QueryType.SAFE to "QueryByEmbedder",
            QueryType.UNSAFE to "QueryByInvoker",
        )

        /**
         * Returns a Pre-Configured Chain with the Defaults for Modifying a POM
         */
        fun createForModify() =
            Chain(CheckDependencyPresent, SimpleUpgrade, SimpleDependencyManagement, SimpleInsert)

        /**
         * returns a pre-configured chain with the defaults for Querying
         */
        fun createForQuery(queryType: QueryType = QueryType.SAFE): Chain {
            val commands: List<Command> = AVAILABLE_QUERY_COMMANDS
                .filter { it.first == queryType }.mapNotNull {
                    val commandClassName = "io.openpixee.maven.operator.${it.second}"

                    try {
                        Class.forName(commandClassName).newInstance() as Command
                    } catch (e: Throwable) {
                        LOGGER.warn("Creating class '{}': ", commandClassName, e)

                        null
                    }
                }
                .toList()

            if (commands.isEmpty())
                throw IllegalStateException("Unable to load any available strategy for ${queryType.name}")

            return Chain(*commands.toTypedArray())
        }
    }
}