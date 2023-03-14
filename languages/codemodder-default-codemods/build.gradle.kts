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
            artifactId = "codemodder-default-codemods"
        }
    }
}

dependencies {
    implementation(project(":languages:codemodder-framework-java"))
    implementation(project(":languages:codemodder-semgrep-provider"))

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)

    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
