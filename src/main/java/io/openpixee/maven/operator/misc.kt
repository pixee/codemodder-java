package io.openpixee.maven.operator

import com.github.zafarkhaja.semver.Version
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.StrSubstitutor
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
    //val siblings = parent.content()

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

internal val PROPERTY_REFERENCE_REGEX = Regex("^\\\$\\{(.*)}$")
internal fun upgradeProperty(c: ProjectModel, propertyName: String) {
    // TODO: Handle Profiles

    if (null == c.resultPom.rootElement.element("properties")) {
        val propertyElement = c.resultPom.rootElement.addElement("properties")

        formatNode(propertyElement)
    }

    val parentPropertyElement = c.resultPom.rootElement.element("properties")

    if (null == parentPropertyElement.element(propertyName)) {
        val newElement = parentPropertyElement.addElement(propertyName)

        formatNode(newElement)
    }

    val propertyElement = parentPropertyElement.element(propertyName)

    propertyElement.text = c.dependency.version

    formatNode(propertyElement)
}

internal fun propertyName(c: ProjectModel, versionNode: Element): String {
    val version = versionNode.textTrim

    if (PROPERTY_REFERENCE_REGEX.matches(version)) {
        val match = PROPERTY_REFERENCE_REGEX.find(version)

        val firstMatch = match!!.groups[1]!!

        return firstMatch.value
    }

    // TODO: Escaping
    // TODO: Template Format (suffix / preffix)?

    return "versions." + c.dependency.artifactId
}

internal fun findOutIfUpgradeIsNeeded(c: ProjectModel, versionNode: Element): Boolean {
    val currentVersionNodeText = resolveVersion(c, versionNode.text!!)

    val currentVersion = Version.valueOf(currentVersionNodeText)
    val newVersion = Version.valueOf(c.dependency.version)

    val versionsAreIncreasing = newVersion.greaterThan(currentVersion)

    return versionsAreIncreasing
}

internal fun resolveVersion(c: ProjectModel, versionText: String): String =
    if (PROPERTY_REFERENCE_REGEX.matches(versionText)) {
        @Suppress("DEPRECATION")
        StrSubstitutor(c.resolvedProperties).replace(versionText)
    } else {
        versionText
    }

internal fun escapedPropertyName(propertyName: String): String =
    "\${$propertyName}"

internal fun upgradeVersionNode(c: ProjectModel, versionNode: Element) {
    if (c.useProperties) {
        val propertyName = propertyName(c, versionNode)

        // define property
        upgradeProperty(c, propertyName)

        versionNode.text = escapedPropertyName(propertyName)
    } else {
        versionNode.text = c.dependency.version
    }
}


