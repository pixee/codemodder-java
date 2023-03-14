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
            artifactId = "codemodder-provider-sarif-semgrep"
        }
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.guice)
    implementation(libs.contrast.sarif)
    implementation(libs.java.security.toolkit)
    implementation(libs.slf4j.api)
    implementation(libs.javaparser.core)
    implementation(project(":languages:codemodder-common"))
    implementation(project(":languages:codemodder-framework-java"))

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.jgit)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
