pluginManagement {
    includeBuild("../gradle/plugins")
}

dependencyResolutionManagement {
    repositories.mavenCentral()
    // For the version catalog 'libs.versions.toml' shared by all builds/components in the repository
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}