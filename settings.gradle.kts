rootProject.name = "codemodder-java"

pluginManagement {
    includeBuild("gradle/build-plugins")
    includeBuild("gradle/build-settings")
}

dependencyResolutionManagement {
    versionCatalogs.create("buildlibs") {
        from(files("gradle/buildlibs.versions.toml"))
    }
}

plugins {
    id("io.codemodder.repositories")
    id("com.gradle.enterprise") version "3.14.1"
}

val isCI = providers.environmentVariable("CI").isPresent

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        isUploadInBackground = !isCI

        if (isCI) {
            publishAlways()
        }

        capture {
            isTaskInputFiles = true
        }
    }
}

includeBuild("core-codemods")
includeBuild("framework")
includeBuild("plugins")
