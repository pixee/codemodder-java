package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.selectXPathNodes
import org.dom4j.Element
import java.io.File

/**
 * ProjectModel represents the input parameters for the chain
 *
 * @todo consider resolution and also Topological Sort of Properties for cross-property reference
 */
class ProjectModel internal constructor(
    val pomFile: POMDocument,

    val parentPomFiles: List<POMDocument> = emptyList(),

    var dependency: Dependency?,
    val skipIfNewer: Boolean,
    val useProperties: Boolean,
    val activeProfiles: Set<String>,
    val overrideIfAlreadyExists: Boolean,
    val queryType: QueryType = QueryType.NONE,

    val repositoryPath: File? = null,

    val offline: Boolean = false,
) {
    internal var modifiedByCommand = false

    /**
     * Involved POM Files
     */
    val allPomFiles: Collection<POMDocument>
        get() = listOfNotNull(
            pomFile,
            *parentPomFiles.toTypedArray()
        )

    val resolvedProperties =
        run {
            val result: MutableMap<String, String> = LinkedHashMap()

            allPomFiles
                .reversed() // parent first, children later - thats why its reversed
                .forEach { pomFile ->
                    val rootProperties =
                        propertiesDefinedOnPomDocument(pomFile)

                    result.putAll(rootProperties)

                    val activatedProfiles = activeProfiles.filterNot { it.startsWith("!") }

                    val newPropertiesFromProfiles = activatedProfiles.map { profileName ->
                        getPropertiesFromProfile(profileName, pomFile)
                    }

                    newPropertiesFromProfiles.forEach { result.putAll(it) }
                }

            result.toMap()
        }

    val propertiesDefinedByFile: Map<String, List<Pair<String, POMDocument>>> =
        run {
            val result: MutableMap<String, List<Pair<String, POMDocument>>> = LinkedHashMap()

            allPomFiles
                .reversed()
                .forEach { pomFile ->
                    val rootProperties =
                        propertiesDefinedOnPomDocument(pomFile)

                    val tempProperties: MutableMap<String, String> = LinkedHashMap()

                    tempProperties.putAll(rootProperties)

                    val activatedProfiles = activeProfiles.filterNot { it.startsWith("!") }

                    val newPropertiesFromProfiles = activatedProfiles.map { profileName ->
                        getPropertiesFromProfile(profileName, pomFile)
                    }

                    newPropertiesFromProfiles.forEach { tempProperties.putAll(it) }

                    tempProperties.entries.forEach { entry ->
                        if (!result.containsKey(entry.key)) {
                            result[entry.key] = ArrayList()
                        }

                        val definitionList =
                            result[entry.key] as MutableList<Pair<String, POMDocument>>

                        definitionList.add(entry.value to pomFile)
                    }
                }

            result
        }

    private fun getPropertiesFromProfile(
        profileName: String,
        pomFile: POMDocument
    ): Map<String, String> {
        val expression =
            "/m:project/m:profiles/m:profile[./m:id[text()='${profileName}']]/m:properties"
        val propertiesElements =
            pomFile.pomDocument.selectXPathNodes(expression)

        val newPropertiesToAppend =
            propertiesElements.filterIsInstance<Element>()
                .flatMap { it.elements() }
                .associate {
                    it.name to it.text
                }

        return newPropertiesToAppend
    }

    companion object {
        fun propertiesDefinedOnPomDocument(pomFile: POMDocument): Map<String, String> {
            val rootProperties =
                pomFile.pomDocument.rootElement.elements("properties")
                    .flatMap { it.elements() }
                    .associate {
                        it.name to it.text
                    }
            return rootProperties
        }
    }
}

