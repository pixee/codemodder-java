pluginManagement {
    // Get our own convention plugins from 'gradle/plugins'
    if (File(rootDir, "gradle/plugins").exists()) {
        includeBuild("gradle/plugins")
    }
    // If not the main build, 'plugins' is located next to the build (e.g. gradle/settings)
    if (File(rootDir, "../plugins").exists()) {
        includeBuild("../plugins")
    }
}

dependencyResolutionManagement {
    repositories.mavenLocal() // TODO remove after security-toolkit has been published somewhere
    repositories.mavenCentral()
}
