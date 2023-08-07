package io.github.pixee.maven.operator

import org.dom4j.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.notExists

object POMScanner {
    private val LOGGER: Logger = LoggerFactory.getLogger(POMScanner::class.java)

    private val RE_WINDOWS_PATH = Regex("""^\p{Alpha}:""")

    @JvmStatic
    fun scanFrom(originalFile: File, topLevelDirectory: File): ProjectModelFactory {
        val originalDocument = ProjectModelFactory.load(originalFile)

        val parentPoms: List<File> = try {
            getParentPoms(originalFile)
        } catch (e: Exception) {
            LOGGER.warn("While trying embedder: ", e)

            return legacyScanFrom(originalFile, topLevelDirectory)
        }

        return originalDocument
            .withParentPomFiles(parentPoms.map { POMDocumentFactory.load(it) })
    }

    private fun legacyScanFrom(originalFile: File, topLevelDirectory: File): ProjectModelFactory {
        val pomFile: POMDocument = POMDocumentFactory.load(originalFile)
        val parentPomFiles: MutableList<POMDocument> = arrayListOf()

        val pomFileQueue: Queue<Element> = LinkedList()

        val relativePathElement =
            pomFile.pomDocument.rootElement.element("parent")?.element("relativePath")

        if (relativePathElement != null && relativePathElement.textTrim.isNotEmpty()) {
            pomFileQueue.add(relativePathElement)
        }

        var lastFile: File = originalFile

        fun resolvePath(baseFile: File, relativePath: String): Path {
            var parentDir = baseFile

            if (parentDir.isFile) {
                parentDir = parentDir.parentFile
            }

            val result = File(File(parentDir, relativePath).toURI().normalize().path)

            lastFile = if (result.isDirectory) {
                result
            } else {
                result.parentFile
            }

            return Paths.get(result.absolutePath)
        }

        val prevPaths: MutableSet<String> = linkedSetOf()

        while (pomFileQueue.isNotEmpty()) {
            val relativePathElement = pomFileQueue.poll()

            if (relativePathElement.textTrim.isEmpty()) {
                break
            }

            val relativePath = fixPomRelativePath(relativePathElement.text)

            if (!isRelative(relativePath))
                throw InvalidPathException(pomFile.file, relativePath)

            if (prevPaths.contains(relativePath)) {
                throw InvalidPathException(pomFile.file, relativePath, loop = true)
            } else {
                prevPaths.add(relativePath)
            }

            val newPath = resolvePath(lastFile, relativePath)

            if (newPath.notExists())
                throw InvalidPathException(pomFile.file, relativePath)

            if (!newPath.startsWith(topLevelDirectory.absolutePath))
                throw InvalidPathException(pomFile.file, relativePath)

            val newPomFile = POMDocumentFactory.load(newPath.toFile())

            parentPomFiles.add(newPomFile)

            val newRelativePathElement =
                newPomFile.pomDocument.rootElement.element("parent")?.element("relativePath")

            if (newRelativePathElement != null) {
                pomFileQueue.add(newRelativePathElement)
            }
        }

        return ProjectModelFactory.loadFor(
            pomFile = pomFile,
            parentPomFiles = parentPomFiles
        )
    }

    private fun fixPomRelativePath(text: String?): String {
        if (null == text)
            return ""

        val name = File(text).name

        if (-1 == name.indexOf(".")) {
            return "$text/pom.xml"
        }

        return text
    }

    private fun isRelative(path: String): Boolean {
        if (path.matches(RE_WINDOWS_PATH)) {
            return false
        }

        return !(path.startsWith("/") || path.startsWith("~"))
    }


    private fun getParentPoms(originalFile: File): List<File> {
        val embedderFacadeResponse = EmbedderFacade.invokeEmbedder(
            EmbedderFacadeRequest(offline = true, pomFile = originalFile)
        )

        val res = embedderFacadeResponse.modelBuildingResult

        val rawModels = res.modelIds.map { res.getRawModel(it) }.toList()

        val parentPoms: List<File> =
            if (rawModels.size > 1) {
                rawModels.subList(1, rawModels.size).mapNotNull { it.pomFile }.toList()
            } else
                emptyList()
        return parentPoms
    }
}
