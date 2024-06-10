plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    api(libs.jackson.yaml)
    api(libs.jackson.core)
}
