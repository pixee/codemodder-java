package io.codemodder.docs

import com.github.javaparser.JavaParser
import com.google.gson.Gson
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.internal.enterprise.test.FileProperty
import java.util.stream.Collectors
import javax.inject.Inject

/**
 * A task to generate documentation for the codemodder plugin
 *
 * This task requires the user to specify command line flag `codemodDocsDir` to specify the directory to write the docs
 */
abstract class GenerateDocsTask : DefaultTask()  {

    @get:Internal
    abstract val codemodDocsDir: DirectoryProperty

    @get:InputFiles
    abstract val javaMainSources: ConfigurableFileCollection

    @get:InputFile
    abstract val defaultCodemodSource: RegularFileProperty

    @Option(option = "codemodDocsDir", description = "The dir to write the docs to")
    fun setCodemodderDocsDir(value: String) {
        codemodDocsDir.set(project.file(value))
    }

    @get:Inject
    abstract val projectLayout: ProjectLayout

    @TaskAction
    fun generateDocs() {
        val codemodDocsDir = this.codemodDocsDir.get().asFile
        if (!codemodDocsDir.exists()) {
            throw IllegalArgumentException("codemodDocsDir does not exist")
        }

        if (codemodDocsDir.list().isEmpty()) {
            println("Docs directory was empty")
        }

        val defaultCodemodSource = projectLayout.projectDirectory.file("src/main/java/io/codemodder/codemods/DefaultCodemods.java").asFile.readText()


        for (javaMainSource in javaMainSources) {
            val codemodFiles = javaMainSource.walkTopDown().filter { it.name.endsWith("Codemod.java") }
            for (codemodFile in codemodFiles) {
                val codemodName = codemodFile.nameWithoutExtension

                // check if it's in the DefaultCodemods list -- we don't do docs for the others
                if (!defaultCodemodSource.contains(codemodName)) {
                    println("Skipping $codemodName")
                    continue
                } else {
                    println("Processing $codemodName")
                }
                val parsedJavaFile = JavaParser().parse(codemodFile).result
                val codemodClass = parsedJavaFile.get().types[0]

                // get the codemod annotation which has some metadata
                val codemodAnnotation = codemodClass.annotations
                    .stream()
                    .filter { it.nameAsString == "Codemod" }
                    .findFirst()
                    .get()
                    .asNormalAnnotationExpr()

                // get the name and other metadata
                val annotationParameters = codemodAnnotation.pairs
                val id = annotationParameters.stream()
                    .filter { it.nameAsString == "id" }
                    .findFirst()
                    .get()
                    .value
                    .asStringLiteralExpr()
                    .value

                val fileName = id.replace(":", "_").replace("/", "_") + ".md"

                val importance = annotationParameters.stream()
                    .filter { it.nameAsString == "importance" }
                    .findFirst()
                    .get()
                    .value
                    .asFieldAccessExpr()
                    .nameAsString

                val mergeGuidance = annotationParameters.stream()
                    .filter { it.nameAsString == "reviewGuidance" }
                    .findFirst()
                    .get()
                    .value
                    .asFieldAccessExpr()
                    .toString()

                // the other metadata is in the resources
                val resourceDir = projectLayout.projectDirectory.dir("src/main/resources/io/codemodder/codemods/$codemodName")
                val description = resourceDir.file("description.md").asFile.readText()
                val reportJson = resourceDir.file("report.json").asFile.readText()
                val report = Gson().fromJson(reportJson, Map::class.java)

                val summary = report["summary"] as String

                // add the scanning tool to the summary
                var needsScanningTool = "No"
                if (id.startsWith("sonar")) {
                    needsScanningTool = "Yes (Sonar)"
                } else if (id.startsWith("codeql")) {
                    needsScanningTool = "Yes (CodeQL)"
                } else if (id.startsWith("semgrep")) {
                    needsScanningTool = "Yes (Semgrep)"
                }

                // get the merge advice
                var mergeGuidanceStr = "Merge After Review"
                if (mergeGuidance == "ReviewGuidance.MERGE_WITHOUT_REVIEW") {
                    mergeGuidanceStr = "Merge Without Review"
                } else if (mergeGuidance == "ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW") {
                    mergeGuidanceStr = "Merge After Cursory Review"
                }

                val mergeGuidanceJustification = report["reviewGuidanceIJustification"]

                val faqs = report["faqs"]

                val references = report["references"] as List<String>
                val referencesStr = references.stream().map { " * [$it]($it)" }.collect(Collectors.joining("\n"))

                val codemodDocsFile = projectLayout.projectDirectory.file("$codemodDocsDir/$fileName")

                // escape the quotes in summary
                val escapedSummary = summary.replace("\"", "\\\"")
                var doc = """
                ---
                title: "$escapedSummary"
                sidebar_position: 1
                ---
                
                ## $id 

                | Importance  | Review Guidance      | Requires Scanning Tool |
                |-------------|----------------------|------------------------|
                | $importance | $mergeGuidanceStr | $needsScanningTool     |
                
            """.trimIndent()

                doc += "\n$description\n"

                val hasFaq = mergeGuidanceJustification != null || faqs != null

                if (hasFaq) {
                    doc += "## F.A.Q.\n\n"
                }

                if (mergeGuidanceJustification != null) {
                    doc += "### Why is this codemod marked as $mergeGuidanceStr?\n\n"
                    doc += "$mergeGuidanceJustification\n\n"
                }

                if (faqs != null) {
                    faqs as List<Map<String, String>>
                    for (faq in faqs) {
                        doc += """
                        ### ${faq["question"]}
                        
                        ${faq["answer"]}
                        
                    """.trimIndent()
                        doc += "\n"
                    }
                }

                doc += "\n## References\n"
                doc += referencesStr
                doc += "\n" // file editors like a trailing newline
                codemodDocsFile.asFile.writeText(doc)
            }
        }
    }

}
