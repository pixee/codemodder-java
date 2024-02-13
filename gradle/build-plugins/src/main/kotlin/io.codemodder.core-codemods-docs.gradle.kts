import com.github.javaparser.JavaParser
import com.google.gson.Gson
import java.util.stream.Collectors

plugins {
    id("io.codemodder.java")
}


/**
 * This task generates markdown documentation for all codemods in the core-codemods project.
 *
 * The documentation is generated from the codemod source files and the codemod resources directory.
 *
 * To run:
 * `./gradlew :core-codemods:generateDocs -PcodemodDocsDir=$DOCS/docs/codemods/java/`
 */
val generateDocs by tasks.registering {
    group = "custom"
    description = "generate markdown docs for all codemods"

    if(!project.properties.containsKey("codemodDocsDir")) {
        throw IllegalArgumentException("codemodDocsDir property is required")
    }

    val codemodDocsDir = file(project.properties["codemodDocsDir"]!!)
    println("Using docs directory: $codemodDocsDir")
    if(!codemodDocsDir.exists()) {
        throw IllegalArgumentException("codemodDocsDir does not exist")
    }

    if(codemodDocsDir.list().isEmpty()) {
        println("Docs directory was empty")
    }

    val javaMainSources = java.sourceSets.main.get().java.srcDirs
    val defaultCodemodSource = file("src/main/java/io/codemodder/codemods/DefaultCodemods.java").readText()

    // for every codemod in the project
    for (javaMainSource in javaMainSources) {
        val codemodFiles = javaMainSource.walkTopDown().filter { it.name.endsWith("Codemod.java") }
        for (codemodFile in codemodFiles) {
            val codemodName = codemodFile.nameWithoutExtension

            // check if it's in the DefaultCodemods list -- we don't do docs for the others
            if(!defaultCodemodSource.contains(codemodName)) {
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

            val fileName = id.replace(":","_").replace("/", "_") + ".md"

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
            val resourceDir = file("src/main/resources/io/codemodder/codemods/$codemodName")
            val description = resourceDir.resolve("description.md").readText()
            val reportJson = resourceDir.resolve("report.json").readText()
            val report = Gson().fromJson(reportJson, Map::class.java)

            val summary = report["summary"] as String

            // add the scanning tool to the summary
            var needsScanningTool = "No"
            if(id.startsWith("sonar")) {
                needsScanningTool = "Yes (Sonar)"
            } else if(id.startsWith("codeql")) {
                needsScanningTool = "Yes (CodeQL)"
            } else if(id.startsWith("semgrep")) {
                needsScanningTool = "Yes (Semgrep)"
            }

            // get the merge advice
            var mergeGuidanceStr = "Merge After Review"
            if(mergeGuidance == "ReviewGuidance.MERGE_WITHOUT_REVIEW") {
                mergeGuidanceStr = "Merge Without Review"
            } else if(mergeGuidance == "ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW") {
                mergeGuidanceStr = "Merge After Cursory Review"
            }

            val mergeGuidanceJustification = report["reviewGuidanceIJustification"]

            val faqs = report["faqs"]

            val references = report["references"] as List<String>
            val referencesStr = references.stream().map { " * [$it]($it)" }.collect(Collectors.joining("\n"))

            val codemodDocsFile = file("$codemodDocsDir/$fileName")

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

            if(hasFaq) {
                doc += "## F.A.Q.\n\n"
            }

            if(mergeGuidanceJustification != null) {
                doc += "### Why is this codemod marked as $mergeGuidanceStr?\n\n"
                doc += "$mergeGuidanceJustification\n\n"
            }

            if(faqs != null) {
                faqs as List<Map<String, String>>
                for(faq in faqs) {
                    doc += """
                        ### ${faq["question"]}
                        
                        ${faq["answer"]}
                        
                        """.trimIndent()
                    doc += "\n"
                }
            }

            doc +=  "\n## References\n"
            doc += referencesStr
            doc += "\n" // file editors like a trailing newline
            codemodDocsFile.writeText(doc)
        }
    }
}
