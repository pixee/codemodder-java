dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
    versionCatalogs.create("testcodelibs") {
        from(files("../gradle/testcodelibs.versions.toml"))
    }
    versionCatalogs.create("testlibs") {
        from(files("../gradle/testlibs.versions.toml"))
    }
}
