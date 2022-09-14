package io.openpixee.maven.operator

fun interface Command {
    fun execute(c: ProjectModel): Boolean
}