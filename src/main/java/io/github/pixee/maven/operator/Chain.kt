package io.github.pixee.maven.operator

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
    internal val commandList: MutableList<Command> = ArrayList(commands.toList())

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

            if (done) {
                if (c.queryType == QueryType.NONE && (!(nextCommand is SupportCommand))) {
                    c.modifiedByCommand = true
                }

                break
            }
        }

        val result = done

        /**
         * Goes Reverse Order applying the filter pattern
         */

        while (listIterator.previousIndex() > 0) {
            val nextCommand = listIterator.previous()

            done = nextCommand.postProcess(c)

            if (done)
                break
        }

        return result
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
            Chain(
                CheckDependencyPresent,
                CheckParentPackaging,
                FormatCommand(),
                DiscardFormatCommand(),
                CompositeDependencyManagement(),
                SimpleUpgrade,
                SimpleDependencyManagement,
                SimpleInsert
            )

        /**
         * returns a pre-configured chain with the defaults for Querying
         */
        fun createForQuery(queryType: QueryType = QueryType.SAFE): Chain {
            val filteredCommands: List<Command> = AVAILABLE_QUERY_COMMANDS
                .filter { it.first == queryType }.mapNotNull {
                    val commandClassName = "io.github.pixee.maven.operator.${it.second}"

                    try {
                        @Suppress("DEPRECATION")
                        Class.forName(commandClassName).newInstance() as Command
                    } catch (e: Throwable) {
                        LOGGER.warn("Creating class '{}': ", commandClassName, e)

                        null
                    }
                }
                .toList()

            val commands : List<Command> = listOf(CHECK_PARENT_DIR_COMMAND) + filteredCommands

            if (commands.isEmpty())
                throw IllegalStateException("Unable to load any available strategy for ${queryType.name}")

            return Chain(*commands.toTypedArray())
        }
    }
}