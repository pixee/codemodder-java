plugins {
    id("io.codemodder.base")
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "pixeeArtifactory"
            url = uri("https://pixee.jfrog.io/artifactory/default-maven-virtual")
            credentials(PasswordCredentials::class)
        }
    }
}
