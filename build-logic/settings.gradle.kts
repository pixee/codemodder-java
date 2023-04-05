dependencyResolutionManagement {
    repositories {
        maven {
            name = "pixeeArtifactory"
            url = uri("https://pixee.jfrog.io/artifactory/default-maven-virtual")
            credentials(PasswordCredentials::class)
        }
    }
    versionCatalogs.create("buildlibs") {
        from(files("gradle/buildlibs.versions.toml"))
    }
}

pluginManagement {
    // we proxy Gradle Plugin Portal
    repositories {
        maven {
            name = "pixeeArtifactory"
            url = uri("https://pixee.jfrog.io/artifactory/default-maven-virtual")
            credentials(PasswordCredentials::class)
        }
    }
}
