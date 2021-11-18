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
