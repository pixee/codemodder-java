pluginManagement {
    // we proxy Gradle Plugin Portal
    repositories {
        mavenCentral()
    }
    includeBuild("../gradle/build-plugins")
}
