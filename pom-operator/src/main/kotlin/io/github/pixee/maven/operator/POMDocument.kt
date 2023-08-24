package io.github.pixee.maven.operator

import org.dom4j.Document
import java.io.File
import java.net.URL
import java.nio.charset.Charset

/**
 * Data Class to Keep track of an entire POM File, including:
 *
 * Path (pomPath)
 *
 * DOM Contents (pomDocument) - original
 * DOM Contents (resultPom) - modified
 *
 * Charset (ditto)
 * Indent (ditto)
 * Preamble (ditto)
 * Suffix (ditto)
 * Line Endings (endl)
 *
 * Original Content (originalPom)
 * Modified Content (resultPomBytes)
 */
@Suppress("ArrayInDataClass")
data class POMDocument(
    val originalPom: ByteArray,
    val pomPath: URL?,
    val pomDocument: Document,
    var charset: Charset = Charset.defaultCharset(),
    var endl: String = "\n",
    var indent: String = "  ",
    var resultPomBytes: ByteArray = byteArrayOf(),

    /**
     * Preamble Contents are stored here
     */
    var preamble: String = "",

    /**
     * Afterword - if needed
     */
    var suffix: String = "",
) {
    internal val file: File get() = File(this.pomPath!!.toURI())

    val resultPom: Document = pomDocument.clone() as Document

    var dirty: Boolean = false

    override fun toString(): String {
        return if (null == this.pomPath) {
            "missing"
        } else {
            ("[POMDocument @ " + this.pomPath.toString() + "]")
        }
    }
}

