package io.openpixee.maven.operator

class Chain(vararg c: Command) {
    private val commandList = ArrayList(c.toList())

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
        fun create() = Chain(SimpleUpgrade, SimpleDependencyManagement, SimpleInsert)
    }
}