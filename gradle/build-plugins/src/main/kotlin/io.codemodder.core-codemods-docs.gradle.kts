import io.codemodder.docs.GenerateDocsTask

plugins {
    id("io.codemodder.java")
}

/**
 * This task generates markdown documentation for all codemods in the core-codemods project.
 *
 * The documentation is generated from the codemod source files and the codemod resources directory.
 *
 * To run:
 * `./gradlew :core-codemods:generateDocs --codemodDocsDir $DOCS/docs/codemods/java/`
 */
tasks.register<GenerateDocsTask>("generateDocs") {
    group = "documentation"
    description = "generate markdown docs for all codemods"
    javaMainSources.from(java.sourceSets.main.get().java.srcDirs)
    defaultCodemodSource.set(layout.projectDirectory.file("src/main/java/io/codemodder/codemods/DefaultCodemods.java"))
}
