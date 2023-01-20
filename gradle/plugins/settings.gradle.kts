pluginManagement {
    includeBuild("../settings")
}

plugins {
    id("io.openpixee.repositories")
}

dependencyResolutionManagement {
    versionCatalogs.create("buildlibs") {
        from(files("../buildlibs.versions.toml"))
    }
}

include("base")