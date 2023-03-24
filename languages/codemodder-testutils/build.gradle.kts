@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
    id("io.openpixee.codetl.base")
    id("io.openpixee.codetl.java-library")
    id("io.openpixee.codetl.maven-publish")
    alias(libs.plugins.fileversioning)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "codemodder-testutils"
        }
    }
}

dependencies {
    api(project(":languages:codemodder-common"))
    api(project(":languages:codemodder-framework-java"))

    implementation(testlibs.bundles.junit.jupiter)
    implementation(testlibs.bundles.hamcrest)
    implementation(testlibs.assertj)
    implementation(testlibs.mockito)

    runtimeOnly(testlibs.junit.jupiter.engine)
}
