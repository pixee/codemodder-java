package io.openpixee.maven.operator

/**
 * Represents a Dependency
 */
data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String? = null,
    val classifier: String? = null,
    val packaging: String? = "jar"
) {
    /**
     * Given a string, parses - and creates - a new dependency Object
     */
    companion object {
        fun fromString(str: String): Dependency {
            val elements = str.split(":")

            if (elements.size < 3)
                throw IllegalStateException("Give me 3 elements")

            return Dependency(elements[0], elements[1], elements[2])
        }
    }
}