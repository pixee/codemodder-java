@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
    id("io.codemodder.base")
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    api("io.github.pixee:codetf-java:0.0.2") // TODO bring codetf-java into the monorepo
    implementation(libs.dom4j)
    api(libs.guice)
    api(libs.contrast.sarif)
    api(libs.java.security.toolkit)
    api(libs.commons.lang3)
    implementation(libs.logback.classic)
    implementation(libs.maven.model)
    api(libs.slf4j.api)
    api(project(":codemodder-common"))

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)

    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
