dependencyResolutionManagement {
    // we're comfortable using this API
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        gradlePluginPortal()

        if (providers.gradleProperty("pixeeArtifactoryUsername").isPresent) {
            maven {
                name = "pixeeArtifactory"
                url = uri("https://pixee.jfrog.io/artifactory/default-maven-virtual")
                credentials(PasswordCredentials::class)
            }
        }
    }
}
