package br.com.ingenieux.pom.operator

import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.Diff
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.IllegalStateException
import javax.annotation.processing.ProcessingEnvironment

open class BaseTest {
    protected fun readPom(path: String, effectivePom: Boolean = false): Document {
        val pomDoc = if (effectivePom) {
            val resourceURI = javaClass.getResource(path).toURI()

            val absPath = File(resourceURI).absolutePath

            val tmpOutputFile = File.createTempFile("tmp-pom", ".xml")

            val psBuilder = ProcessBuilder(
                "mvn",
                "-N",
                "-o",
                "-f",
                absPath,
                "help:effective-pom",
                "-Doutput=${tmpOutputFile.absolutePath}"
            ).inheritIO()

            psBuilder.environment().putAll(System.getenv())

            val process = psBuilder.start()

            val retCode = process.waitFor()

            if (0 != retCode)
                throw IllegalStateException("Unexpected return code from maven: $retCode")

            InputStreamReader(FileInputStream(tmpOutputFile))
        } else {
            InputStreamReader(javaClass.getResourceAsStream(path))
        }

        return SAXReader().read(pomDoc)!!
    }

    protected fun getDifferences(
        original: Document,
        modified: Document
    ): Diff? {
        val originalDoc = Input.fromString(original.asXML()).build()
        val modifiedDoc = Input.fromString(modified.asXML()).build()

        val diff = DiffBuilder.compare(originalDoc).withTest(modifiedDoc).build()
        return diff
    }
}
