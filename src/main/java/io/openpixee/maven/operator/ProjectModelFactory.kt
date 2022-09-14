package io.openpixee.maven.operator

import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL

class ProjectModelFactory private constructor(
    private var pomDocument: Document,
    private var dependency: Dependency? = null,
    private var skipIfNewer: Boolean = false,
    private var useProperties: Boolean = false,
    private var activeProfiles: Set<String> = emptySet(),
) {
    fun withDependency(dep: Dependency): ProjectModelFactory = this.apply {
        this.dependency = dep
    }

    fun withSkipIfNewer(skipIfNewer: Boolean): ProjectModelFactory = this.apply {
        this.skipIfNewer = skipIfNewer
    }

    fun withUseProperties(useProperties: Boolean): ProjectModelFactory = this.apply {
        this.useProperties = useProperties
    }

    fun withActiveProfiles(vararg activeProfiles: String): ProjectModelFactory = this.apply {
        this.activeProfiles = setOf(*activeProfiles)
    }

    fun build(): ProjectModel {
        return ProjectModel(
            pomDocument = pomDocument,
            dependency = dependency!!,
            skipIfNewer = skipIfNewer,
            useProperties = useProperties,
            activeProfiles = activeProfiles
        )
    }

    companion object {
        private fun load(`is`: InputStream): ProjectModelFactory {
            val pomDocument = SAXReader().read(`is`)!!

            return ProjectModelFactory(pomDocument)
        }

        @JvmStatic
        fun load(f: File) =
            load(FileInputStream(f))

        @JvmStatic
        fun load(url: URL) =
            load(url.openStream())
    }
}