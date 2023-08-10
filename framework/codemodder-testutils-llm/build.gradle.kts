plugins {
    id("io.codemodder.base")
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

dependencies {
    implementation("io.codemodder:codemodder-core")
    implementation("io.codemodder:codemodder-plugin-llm")
    implementation("io.codemodder:codemodder-testutils")
    implementation(testlibs.bundles.hamcrest)
    implementation(testlibs.bundles.junit.jupiter)
}
