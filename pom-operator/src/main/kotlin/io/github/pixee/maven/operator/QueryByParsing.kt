@file:Suppress("DEPRECATION")

package io.github.pixee.maven.operator

import org.apache.commons.lang3.builder.CompareToBuilder
import org.apache.commons.lang3.text.StrSubstitutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

/**
 * this one is a bit more complex, as it intents to to a "best effort" attempt at parsing a pom
 * focusing only on dependency right now,  * without relying to any maven infrastructure at all
 */
class QueryByParsing : AbstractQueryCommand() {
    override fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModel) {
        TODO("Not yet implemented")
    }

    private val dependencies: MutableSet<Dependency> = LinkedHashSet()

    private val dependencyManagement: MutableSet<Dependency> =
        TreeSet(object : Comparator<Dependency> {
            override fun compare(o1: Dependency?, o2: Dependency?): Int {
                if (o1 == o2)
                    return 0

                if (o1 == null)
                    return 1

                if (o2 == null)
                    return -1

                return CompareToBuilder().append(o1.groupId, o2.groupId)
                    .append(o1.artifactId, o2.artifactId).toComparison()
            }
        })

    private val properties: MutableMap<String, String> = LinkedHashMap()

    private val strSubstitutor = StrSubstitutor(properties)

    override fun execute(pm: ProjectModel): Boolean {
        /**
         * Enlist all pom files given an hierarchy
         */
        val pomFilesByHierarchy = pm.allPomFiles.reversed()

        for (pomDocument in pomFilesByHierarchy) {
            updateProperties(pomDocument)

            updateDependencyManagement(pomDocument)

            updateDependencies(pomDocument)
        }

        this.result = dependencies

        return true
    }

    private fun updateDependencyManagement(pomDocument: POMDocument) {
        val dependencyManagementDependenciesToAdd: Collection<Dependency> =
            pomDocument.pomDocument.//
            rootElement. //
            element("dependencyManagement")?. //
            element("dependencies")?. //
            elements("dependency")?. //
            map { dependencyElement ->
                val groupId = dependencyElement.element("groupId").text
                val artifactId = dependencyElement.element("artifactId").text

                val version = dependencyElement.element("version")?.text ?: "UNKNOWN"

                val classifier = dependencyElement.element("classifier")?.text
                val packaging = dependencyElement.element("packaging")?.text

                val versionInterpolated = try {
                    strSubstitutor.replace(version)
                } catch (e: java.lang.IllegalStateException) {
                    LOGGER.warn("while interpolating version", e)

                    "UNKNOWN"
                }

                Dependency(groupId, artifactId, versionInterpolated, classifier, packaging)
            }?.toList() ?: emptyList()

        this.dependencyManagement.addAll(dependencyManagementDependenciesToAdd)
    }

    fun lookForDependencyManagement(groupId: String, artifactId: String): Dependency? =
        this.dependencyManagement.firstOrNull { it.groupId == groupId && it.artifactId == artifactId }

    private fun updateDependencies(pomDocument: POMDocument) {
        val dependenciesToAdd: Collection<Dependency> =
            pomDocument.pomDocument.//
            rootElement. //
            element("dependencies")?. //
            elements("dependency")?. //
            map { dependencyElement ->
                val groupId = dependencyElement.element("groupId").text
                val artifactId = dependencyElement.element("artifactId").text

                val versionElement = dependencyElement.element("version")

                val proposedDependency = lookForDependencyManagement(groupId, artifactId)

                if (versionElement == null && null != proposedDependency) {
                    proposedDependency
                } else {
                    val version = versionElement?.text ?: "UNKNOWN"

                    val classifier = dependencyElement.element("classifier")?.text
                    val packaging = dependencyElement.element("packaging")?.text

                    val versionInterpolated = try {
                        strSubstitutor.replace(version)
                    } catch (e: java.lang.IllegalStateException) {
                        LOGGER.warn("while interpolating version", e)

                        "UNKNOWN"
                    }

                    Dependency(groupId, artifactId, versionInterpolated, classifier, packaging)
                }
            }?.toList() ?: emptyList()

        this.dependencies.addAll(
            dependenciesToAdd
        )
    }

    /**
     * Updates the Properties member variable based on whats on the POMDocument
     */
    private fun updateProperties(pomDocument: POMDocument) {
        val propsDefined = ProjectModel.propertiesDefinedOnPomDocument(pomDocument)

        propsDefined.entries.filterNot { it.value.matches(RE_INTERPOLATION) }
            .forEach {
                properties[it.key] = it.value
            }

        propsDefined.entries.filterNot { it.value.matches(RE_INTERPOLATION) }
            .forEach {
                val newValue = try {
                    strSubstitutor.replace(it.value)
                } catch (e: IllegalStateException) {
                    LOGGER.warn("while replacing variables: ", e)

                    it.value
                }

                properties.put(it.key, it.value)
            }
    }

    companion object {
        val RE_INTERPOLATION = Regex(""".*\$\{[\p{Alnum}.-_]+\}.*""")

        val logger: Logger = LoggerFactory.getLogger(QueryByParsing::class.java)
    }
}