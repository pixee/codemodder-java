dependencyResolutionManagement {
    repositories.mavenCentral()

    versionCatalogs.create("libs") {
        from(files("../libs.versions.toml"))
    }

    versionCatalogs.create("buildlibs") {
        from(files("./buildlibs.versions.toml"))
    }
}

include("base")