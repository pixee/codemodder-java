pluginManagement {
    repositories.gradlePluginPortal()
}

dependencyResolutionManagement {
    versionCatalogs.create("buildlibs") {
        from(files("../buildlibs.versions.toml"))
    }
    repositories.mavenCentral()
}
