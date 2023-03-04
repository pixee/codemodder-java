package io.openpixee.maven.operator

import org.apache.commons.io.IOUtils
import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.charset.Charset

/**
 * Builder Object for ProjectModel instances
 */
class ProjectModelFactory private constructor(
    private val originalPom: ByteArray,
    private var pomPath: URL?,
    private var pomDocument: Document,
    private var dependency: Dependency? = null,
    private var skipIfNewer: Boolean = false,
    private var useProperties: Boolean = false,
    private var activeProfiles: Set<String> = emptySet(),
    private var overrideIfAlreadyExists: Boolean = false,
    private var queryType: QueryType = QueryType.NONE,
) {
    /**
     * Fluent Setter
     * @param pomPath pomPath
     */
    fun withPomPath(pomPath: URL): ProjectModelFactory = this.apply {
        this.pomPath = pomPath
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
     */
    fun build(): ProjectModel {
        return ProjectModel(
            originalPom = originalPom,
            pomPath = pomPath,
            pomDocument = pomDocument,
            dependency = dependency,
            skipIfNewer = skipIfNewer,
            useProperties = useProperties,
            activeProfiles = activeProfiles,
            overrideIfAlreadyExists = overrideIfAlreadyExists,
            queryType = queryType,
            charset = Charset.defaultCharset(),
            endl = "",
            indent = "",
        )
    }

    companion object {
        @JvmStatic
        fun load(`is`: InputStream): ProjectModelFactory {
            val originalPom: ByteArray = IOUtils.toByteArray(`is`)

            val pomDocument = SAXReader().read(originalPom.inputStream())!!

            return ProjectModelFactory(pomPath = null, pomDocument = pomDocument, originalPom = originalPom)
        }

        @JvmStatic
        fun load(f: File) =
            load(f.toURI().toURL())

        @JvmStatic
        fun load(url: URL): ProjectModelFactory {
            val originalPom: ByteArray = IOUtils.toByteArray(url.openStream())

            val pomDocument = SAXReader().read(originalPom.inputStream())

            return ProjectModelFactory(pomPath = url, pomDocument = pomDocument, originalPom = originalPom)
        }
    }
}