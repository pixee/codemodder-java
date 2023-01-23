include("plugin-analysis-plugins", "settings")

dependencyResolutionManagement {
    versionCatalogs.create("buildlibs") {
        from(files("../buildlibs.versions.toml"))
    }
    repositories.mavenCentral()
}
