plugins {
    id("java-library")
    id("maven-publish")
}

group = "ai.pixee.triage"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    //api(platform("ai.pixee.platform.platforms:quarkus-aws-platform"))
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-annotations")
    //api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    api("info.picocli:picocli:4.7.6")
    api("com.contrastsecurity:java-sarif:2.0")
    //api("ai.pixee.platform.common:openai")
    //implementation("org.slf4j:slf4j-api")
    //testImplementation(platform("ai.pixee.platform.platforms:junit-platform"))
    testImplementation("io.github.artsok:rerunner-jupiter:2.1.6")
    //testImplementation("org.mockito:mockito-core")
    //testImplementation("org.assertj:assertj-core")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "local"
            url = uri("${project.rootDir}/../local-repo")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
