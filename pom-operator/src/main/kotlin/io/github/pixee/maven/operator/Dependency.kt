package io.github.pixee.maven.operator

/**
 * Represents a Dependency
 */
data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String? = null,
    val classifier: String? = null,
    val packaging: String? = "jar",
    val scope: String? = "compile",
) {
    override fun toString(): String {
        return listOf(groupId, artifactId, packaging, version).joinToString(":")
    }
    /**
     * Given a string, parses - and creates - a new dependency Object
     */
    companion object {
        fun fromString(str: String): Dependency {
            val elements = str.split(":")

            if (elements.size < 3)
                throw IllegalStateException("Give me at least 3 elements")

            return Dependency(elements[0], elements[1], elements[2])
        }
    }
}