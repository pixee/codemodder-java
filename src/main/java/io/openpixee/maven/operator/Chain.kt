package io.openpixee.maven.operator

/**
 * Implements a Chain of Responsibility Pattern
 *
 * @constructor commands: Commands to Use
 */
class Chain(vararg commands: Command) {
    /**
     * Internal ArrayList of the Commands
     */
    private val commandList = ArrayList(commands.toList())

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
        /**
         * Returns a Pre-Configured Chain with the Defaults
         */
        fun create() = Chain(SimpleUpgrade, SimpleDependencyManagement, SimpleInsert)
    }
}