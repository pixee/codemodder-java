rootProject.name = "codemodder-java"

pluginManagement {
    includeBuild("gradle/build-plugins")
    includeBuild("gradle/build-settings")
}

dependencyResolutionManagement {
    versionCatalogs.create("buildlibs") {
        from(files("gradle/buildlibs.versions.toml"))
    }
    versionCatalogs.create("testlibs") {
        from(files("gradle/testlibs.versions.toml"))
    }
}

plugins {
    id("io.codemodder.repositories")
    id("com.gradle.enterprise") version "3.14.1"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
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

include("core-codemods")
include("examples")
include("framework:codemodder-base")
include("framework:codemodder-testutils")
include("framework:codemodder-testutils-llm")
include("plugins:codemodder-plugin-aws")
include("plugins:codemodder-plugin-codeql")
include("plugins:codemodder-plugin-llm")
include("plugins:codemodder-plugin-maven")
include("plugins:codemodder-plugin-pmd")
include("plugins:codemodder-plugin-semgrep")
