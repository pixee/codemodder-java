package io.openpixee.maven.operator

import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.IllegalStateException
import java.net.URL

data class ProjectModel(
    val pomDocument: Document,
    val dependencyToInsert: Dependency,
    val skipIfNewer: Boolean,
) {
    val resultPom: Document = pomDocument.clone() as Document

    fun getEffectivePom(): Document {
        val tmpInputFile = File.createTempFile("tmp-pom-orig", ".xml")

        tmpInputFile.writeText(resultPom.asXML())

        val tmpOutputFile = File.createTempFile("tmp-pom", ".xml")

        val psBuilder = ProcessBuilder(
            "mvn",
            "-N",
            //"-o",
            "-f",
            tmpInputFile.absolutePath,
            "help:effective-pom",
            "-Doutput=${tmpOutputFile.absolutePath}"
        ).inheritIO()

        psBuilder.environment().putAll(System.getenv())

        val process = psBuilder.start()

        val retCode = process.waitFor()

        if (0 != retCode)
            throw IllegalStateException("Unexpected return code from maven: $retCode")

        return SAXReader().read(FileInputStream(tmpOutputFile))
    }
}

object ProjectModelFactory {
    var pomDocument: Document? = null

    var dependency: Dependency? = null

    var skipIfNewer: Boolean = false

    private fun load(`is`: InputStream): ProjectModelFactory {
        val pomDocument = SAXReader().read(`is`)!!

        return this.apply {
            this.pomDocument = pomDocument
        }
    }

    fun withDependency(dep: Dependency): ProjectModelFactory {
        return this.apply {
            this.dependency = dep
        }
    }

    fun withSkipIfNewer(skipIfNewer: Boolean): ProjectModelFactory {
        return this.apply {
            this.skipIfNewer = skipIfNewer
        }
    }


    public fun build(): ProjectModel {
        return ProjectModel(pomDocument!!, dependency!!, skipIfNewer)
    }

    @JvmStatic
    fun load(f: File) =
        load(FileInputStream(f))

    @JvmStatic
    fun load(url: URL) =
        load(url.openStream())
}