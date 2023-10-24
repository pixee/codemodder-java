import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "3.1.4"
}

group = "ai.pixee.integration"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.1.4")
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("test-app.jar")
}

