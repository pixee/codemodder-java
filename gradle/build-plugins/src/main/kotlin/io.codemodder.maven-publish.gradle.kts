plugins {
    id("io.codemodder.base")
    id("de.epitschke.gradle-file-versioning")
    `maven-publish`
}

publishing {
    repositories {
        mavenCentral()
    }

    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
}
