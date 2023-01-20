rootProject.name = "codetl"

pluginManagement {
    includeBuild("gradle/settings")
}

plugins {
    id("io.openpixee.repositories")
}

dependencyResolutionManagement {
    versionCatalogs.create("testcodelibs") {
        from(files("./gradle/testcodelibs.versions.toml"))
    }

    versionCatalogs.create("testlibs") {
        from(files("./gradle/testlibs.versions.toml"))
    }
}

include("cli", "languages:java", "languages:javascript")
