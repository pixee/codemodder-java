plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

description = "Base framework for writing codemods in Java"

// add the project version to the manifest
tasks.jar {
    manifest {
        attributes["Implementation-Version"] = version
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    implementation(libs.dom4j)
    api(libs.guice)
    api(libs.contrast.sarif)
    api(libs.java.security.toolkit)
    api(libs.commons.lang3)

    api("io.codemodder:codetf-java:3.0.1")
    api(libs.slf4j.api)
    api(libs.javaparser.core)
    api(libs.javaparser.symbolsolver.core)
    api(libs.javaparser.symbolsolver.logic)
    api(libs.javaparser.symbolsolver.model)
    api(libs.javadiff)
    api(libs.jtokkit)
    api(libs.openai.service)
    api("io.github.classgraph:classgraph:4.8.160")

    implementation(libs.tuples)
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.maven.model)
    implementation(libs.picocli)
    implementation(libs.juniversalchardet)

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)

    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
