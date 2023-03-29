rootProject.name = "codemodder-java"

pluginManagement {
    includeBuild("gradle/build-plugins")
    includeBuild("gradle/build-settings")
}

plugins {
    id("io.codemodder.repositories")
}

includeBuild("codemodder-community-codemods")
includeBuild("framework")
includeBuild("legacy")
includeBuild("plugins")
