package io.github.pixee.maven.operator

import java.io.File
import java.io.InputStream
import java.net.URL

/**
 * Builder Object for ProjectModel instances
 */
class ProjectModelFactory private constructor(
    private var pomFile: POMDocument,
    private var parentPomFiles: List<POMDocument> = listOf(),
    private var dependency: Dependency? = null,
    private var skipIfNewer: Boolean = false,
    private var useProperties: Boolean = false,
    private var activeProfiles: Set<String> = emptySet(),
    private var overrideIfAlreadyExists: Boolean = false,
    private var queryType: QueryType = QueryType.NONE,
    private var repositoryPath: File? = null,
) {
    /**
     * Fluent Setter
     *
     * @param pomFile POM File
     */
    fun withPomFile(pomFile: POMDocument): ProjectModelFactory = this.apply {
        this.pomFile = pomFile
    }

    /**
     * Fluent Setter
     *
     * @param parentPomFiles Parent POM Files
     */
    fun withParentPomFiles(parentPomFiles: Collection<POMDocument?>): ProjectModelFactory =
        this.apply {
            this.parentPomFiles = listOf(*parentPomFiles.filterNotNull().toTypedArray())
        }

    /**
     * Fluent Setter
     *
     * @param dep dependency
     */
    fun withDependency(dep: Dependency): ProjectModelFactory = this.apply {
        this.dependency = dep
    }

    /**
     * Fluent Setter
     */
    fun withSkipIfNewer(skipIfNewer: Boolean): ProjectModelFactory = this.apply {
        this.skipIfNewer = skipIfNewer
    }

    /**
     * Fluent Setter
     */
    fun withUseProperties(useProperties: Boolean): ProjectModelFactory = this.apply {
        this.useProperties = useProperties
    }

    /**
     * Fluent Setter
     */
    fun withActiveProfiles(vararg activeProfiles: String): ProjectModelFactory = this.apply {
        this.activeProfiles = setOf(*activeProfiles)
    }

    /**
     * Fluent Setter
     */
    fun withOverrideIfAlreadyExists(overrideIfAlreadyExists: Boolean) = this.apply {
        this.overrideIfAlreadyExists = overrideIfAlreadyExists
    }

    /**
     * Fluent Setter
     *
     * @param queryType query type
     */
    fun withQueryType(queryType: QueryType) = this.apply {
        this.queryType = queryType
    }

    /**
     * Fluent Setter
     *
     * @param repositoryPath Repository Path
     */
    fun withRepositoryPath(repositoryPath: File?) = this.apply {
        this.repositoryPath = repositoryPath
    }

    /**
     * Fluent Setter
     */
    fun build(): ProjectModel {
        return ProjectModel(
            pomFile = pomFile,
            parentPomFiles = parentPomFiles,
            dependency = dependency,
            skipIfNewer = skipIfNewer,
            useProperties = useProperties,
            activeProfiles = activeProfiles,
            overrideIfAlreadyExists = overrideIfAlreadyExists,
            queryType = queryType,
            repositoryPath = repositoryPath,
        )
    }

    /**
     * Mostly Delegates to POMDocumentFactory
     */
    companion object {
        @JvmStatic
        fun load(`is`: InputStream): ProjectModelFactory {
            val pomDocument = POMDocumentFactory.load(`is`)

            return ProjectModelFactory(pomFile = pomDocument)
        }

        @JvmStatic
        fun load(f: File) =
            load(f.toURI().toURL())

        @JvmStatic
        fun load(url: URL): ProjectModelFactory {
            val pomFile = POMDocumentFactory.load(url)

            return ProjectModelFactory(pomFile = pomFile)
        }

        @JvmStatic
        internal fun loadFor(
            pomFile: POMDocument,
            parentPomFiles: Collection<POMDocument>,
        ) = ProjectModelFactory(pomFile = pomFile, parentPomFiles = parentPomFiles.toList())
    }
}