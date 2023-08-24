package io.github.pixee.maven.operator

/**
 * This is an exception to tag when the output file couldn't be generated - perhaps due a missing or incompatible maven installation
 */
internal class InvalidContextException : RuntimeException()