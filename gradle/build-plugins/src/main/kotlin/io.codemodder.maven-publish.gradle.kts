plugins {
    id("io.codemodder.base")
    id("de.epitschke.gradle-file-versioning")
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
