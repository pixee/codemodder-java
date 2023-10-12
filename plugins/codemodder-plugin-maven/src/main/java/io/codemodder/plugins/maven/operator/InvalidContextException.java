package io.codemodder.plugins.maven.operator;

/**
 * This is an exception to tag when the output file couldn't be generated - perhaps due a missing or
 * incompatible maven installation
 */
class InvalidContextException extends RuntimeException {}
