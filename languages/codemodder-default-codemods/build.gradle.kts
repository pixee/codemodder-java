@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
    id("io.openpixee.codetl.base")
    id("io.openpixee.codetl.java-library")
    id("io.openpixee.codetl.maven-publish")
    id("application")
    alias(libs.plugins.fileversioning)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    mainClass.set("io.codemodder.codemods.Runner")
}

spotless {
    java {
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
    implementation(libs.javax.inject)
    implementation(libs.contrast.sarif)
    implementation(libs.slf4j.api)
    implementation(libs.javaparser.core)
    implementation(libs.javaparser.symbolsolver.core)
    implementation(libs.javaparser.symbolsolver.logic)
    implementation(libs.javaparser.symbolsolver.model)
    implementation(project(":languages:codemodder-common"))
    implementation(project(":languages:codemodder-framework-java"))
    implementation(project(":languages:codemodder-semgrep-provider"))

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)

    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
