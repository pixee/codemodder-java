dependencyResolutionManagement {
    repositories {
        if (providers.gradleProperty("pixeeBuild").isPresent) {
            maven {
                name = "pixeeArtifactory"
                url = uri("https://pixee.jfrog.io/artifactory/default-maven-virtual")
                credentials(PasswordCredentials::class)
            }
        } else {
            mavenCentral()
            gradlePluginPortal()
        }
    }
}
