pluginManagement {
    includeBuild("../settings")
}

plugins {
    // only take repositories, because the other settings plugins are intended for the root build
    id("io.openpixee.codetl.repositories")
}

dependencyResolutionManagement {
    versionCatalogs.create("buildlibs") {
        from(files("../buildlibs.versions.toml"))
    }
}
