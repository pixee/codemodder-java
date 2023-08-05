plugins {
    id("io.codemodder.base")
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    api(project(":codemodder-base"))

    implementation(testlibs.bundles.junit.jupiter)
    implementation(testlibs.bundles.hamcrest)
    implementation(testlibs.assertj)
    implementation(testlibs.mockito)

    runtimeOnly(testlibs.junit.jupiter.engine)
}
