pluginManagement {
    // we proxy Gradle Plugin Portal
    repositories {
        maven {
            name = "pixeeArtifactory"
            url = uri("https://pixee.jfrog.io/artifactory/default-maven-virtual")
            credentials(PasswordCredentials::class)
        }
    }
    includeBuild("../gradle/build-plugins")
}
