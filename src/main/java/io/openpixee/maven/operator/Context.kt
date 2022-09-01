package io.openpixee.maven.operator

import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.IllegalStateException
import java.net.URL

data class Context(
    val pomDocument: Document,
    val dependencyToInsert: Dependency,
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

    companion object {
        private fun load(`is`: InputStream, dependencyToInsert: Dependency): Context {
            val pomDocument = SAXReader().read(`is`)!!

            return Context(pomDocument, dependencyToInsert)
        }

        fun load(f: File, dependencyToInsert: Dependency) =
            load(FileInputStream(f), dependencyToInsert)

        fun load(url: URL, dependencyToInsert: Dependency) =
            load(url.openStream(), dependencyToInsert)
    }
}