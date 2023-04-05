dependencyResolutionManagement {
    versionCatalogs.create("libs") {
        from(files("../build-logic/gradle/libs.versions.toml"))
    }
    versionCatalogs.create("testcodelibs") {
        from(files("../build-logic/gradle/testcodelibs.versions.toml"))
    }
    versionCatalogs.create("testlibs") {
        from(files("../build-logic/gradle/testlibs.versions.toml"))
    }
}
