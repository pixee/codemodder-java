package io.openpixee.maven.operator

import org.apache.commons.lang3.StringUtils
import org.dom4j.Element
import org.dom4j.Text
import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.dom4j.tree.DefaultText
import java.io.StringReader
import java.io.StringWriter
import kotlin.math.ceil

internal fun formatNode(node: Element) {
    val parent = node.parent
    val siblings = parent.content()

    val indentLevel = findIndentLevel(node)

    val clonedNode = node.clone() as Element

    val out = StringWriter()

    val outputFormat = OutputFormat.createPrettyPrint()

    val xmlWriter = XMLWriter(out, outputFormat)

    xmlWriter.setIndentLevel(ceil(indentLevel.toDouble() / 2).toInt())

    xmlWriter.write(clonedNode)

    val content = out.toString()

    val newElement = SAXReader().read(StringReader(content)).rootElement.clone() as Element

    parent.remove(node)

    parent.add(DefaultText("\n" + StringUtils.repeat(" ", indentLevel)))
    parent.add(newElement)
    parent.add(DefaultText("\n" + StringUtils.repeat(" ", ((indentLevel - 1) / 2))))
}

internal fun findIndentLevel(node: Element): Int {
    val siblings = node.parent.content()
    val myIndex = siblings.indexOf(node)

    if (myIndex > 0) {
        val lastElement = siblings.subList(0, myIndex).findLast {
            (it is Text) && it.text.matches(Regex("\\n+\\s+"))
        }

        val lastElementText = lastElement?.text ?: ""

        return lastElementText.trimStart('\n').length
    }

    return 0
}
