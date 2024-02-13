pluginManagement {
    includeBuild("../build-settings")
}

plugins {
    // only take repositories, because the other settings plugins are intended for the root build
    id("io.codemodder.repositories")
}

dependencyResolutionManagement {
    versionCatalogs.create("buildlibs") {
        from(files("../buildlibs.versions.toml"))
    }

    versionCatalogs.create("libs") {
        from(files("../libs.versions.toml"))
    }
}
