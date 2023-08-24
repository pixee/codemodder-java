package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.selectXPathNodes
import org.dom4j.Element
import org.dom4j.Text

/**
 * Guard Command Singleton use to validate required parameters
 */
val CheckParentPackaging = object : AbstractCommand() {
    fun packagingTypePredicate(d: POMDocument, packagingType: String): Boolean {
        val elementText =
            d.pomDocument.rootElement.selectXPathNodes("/m:project/m:packaging/text()")
                .firstOrNull()

        if (elementText is Text) {
            return elementText.text.equals(packagingType)
        }

        return false
    }

    override fun execute(pm: ProjectModel): Boolean {
        val wrongParentPoms = pm.parentPomFiles.filterNot { packagingTypePredicate(it, "pom") }

        if (wrongParentPoms.isNotEmpty()) {
            throw WrongDependencyTypeException("wrong packaging type for parentPom")
        }

        if (pm.parentPomFiles.isNotEmpty()) {
            // check main pom file has a inheritance to one of the members listed
            if (!hasValidParentAndPackaging(pm.pomFile)) {
                throw WrongDependencyTypeException("invalid parent/packaging combo for main pomfile")
            }
        }

        // todo: test a->b->c

        return false
    }

    private fun hasValidParentAndPackaging(pomFile: POMDocument): Boolean {
        val parentNode = pomFile.pomDocument.rootElement.selectXPathNodes("/m:project/m:parent")
            .firstOrNull() as Element? ?: return false

        val packagingText =
            (pomFile.pomDocument.rootElement.selectXPathNodes("/m:project/m:packaging/text()")
                .firstOrNull() as Text?)?.text ?: "jar"

        @Suppress("UnnecessaryVariable") val validPackagingType = packagingText.endsWith("ar")

        return validPackagingType
    }
}