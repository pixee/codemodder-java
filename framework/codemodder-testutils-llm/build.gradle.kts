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
    implementation("io.codemodder:codemodder-core")
    implementation("io.codemodder:codemodder-plugin-llm")
    implementation("io.codemodder:codemodder-testutils")
    implementation(testlibs.bundles.hamcrest)
    implementation(testlibs.bundles.junit.jupiter)
}
