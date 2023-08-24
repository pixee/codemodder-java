package io.github.pixee.maven.operator

import org.apache.commons.io.IOUtils
import org.dom4j.io.SAXReader
import java.io.File
import java.io.InputStream
import java.net.URL

/**
 * Factory for a POMDocument
 */
object POMDocumentFactory {
    @JvmStatic
    fun load(`is`: InputStream): POMDocument {
        val originalPom: ByteArray = IOUtils.toByteArray(`is`)
        val pomDocument = SAXReader().read(originalPom.inputStream())!!

        return POMDocument(originalPom = originalPom, pomDocument = pomDocument, pomPath = null)
    }

    @JvmStatic
    fun load(f: File) =
        load(f.toURI().toURL())

    @JvmStatic
    fun load(url: URL): POMDocument {
        val originalPom: ByteArray = IOUtils.toByteArray(url.openStream())

        val saxReader = SAXReader()

        val pomDocument = saxReader.read(originalPom.inputStream())

        return POMDocument(originalPom = originalPom, pomPath = url, pomDocument = pomDocument)
    }

}