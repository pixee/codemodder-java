package io.openpixee.maven.operator

import io.openpixee.maven.operator.util.Util.selectXPathNodes
import org.dom4j.Document
import org.dom4j.Element

/**
 * ProjectModel represents the input parameters for the chain
 *
 * @todo Wrap it into a <pre>Context</pre> interface
 */
class ProjectModel internal constructor(
    val pomDocument: Document,
    val dependency: Dependency,
    val skipIfNewer: Boolean,
    val useProperties: Boolean,
    val activeProfiles: Set<String>,
    val overrideIfAlreadyExists: Boolean,
) {
    val resultPom: Document = pomDocument.clone() as Document

    val resolvedProperties: Map<String, String> =
        run {
            val rootProperties =
                pomDocument.rootElement.elements("properties").flatMap { it.elements() }
                    .associate {
                        it.name to it.text
                    }
            val result: MutableMap<String, String> = LinkedHashMap()
            result.putAll(rootProperties)
            val activatedProfiles = activeProfiles.filterNot { it.startsWith("!") }
            activatedProfiles.forEach { profileName ->
                val expression =
                    "/m:project/m:profiles/m:profile[./m:id[text()='${profileName}']]/m:properties"
                val propertiesElements =
                    pomDocument.selectXPathNodes(expression)

                val newPropertiesToAppend =
                    propertiesElements.filter { it is Element }.map { it as Element }
                        .flatMap { it.elements() }
                        .associate {
                            it.name to it.text
                        }

                result.putAll(newPropertiesToAppend)
            }
            result.toMap()
        }
}

