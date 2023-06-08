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
    api(project(":codemodder-core"))
    api("io.codemodder:codemodder-plugin-semgrep")
    api("io.codemodder:codemodder-plugin-codeql")

    implementation(testlibs.bundles.junit.jupiter)
    implementation(testlibs.bundles.hamcrest)
    implementation(testlibs.assertj)
    implementation(testlibs.mockito)

    runtimeOnly(testlibs.junit.jupiter.engine)
}
