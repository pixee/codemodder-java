rootProject.name = "codemodder-java"

pluginManagement {
    includeBuild("gradle/build-plugins")
    includeBuild("gradle/build-settings")
}

dependencyResolutionManagement {
    versionCatalogs.create("buildlibs") {
        from(files("gradle/buildlibs.versions.toml"))
    }
}

plugins {
    id("io.codemodder.repositories")
}

includeBuild("core-codemods")
includeBuild("framework")
includeBuild("plugins")
