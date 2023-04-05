rootProject.name = "codemodder-java"

pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    versionCatalogs.create("buildlibs") {
        from(files("build-logic/gradle/buildlibs.versions.toml"))
    }
}

plugins {
    id("io.codemodder.repositories")
}

includeBuild("codemodder-community-codemods")
includeBuild("framework")
includeBuild("legacy")
includeBuild("plugins")
