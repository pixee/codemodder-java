plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

dependencies {
    api(libs.jackson.yaml)
    api(libs.jackson.core)
}

description = "Library with API models for data from Sonar tools"
