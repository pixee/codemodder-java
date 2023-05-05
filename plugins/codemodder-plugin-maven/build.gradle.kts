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
    implementation("io.codemodder:codemodder-core")
    implementation("io.openpixee.maven:pom-operator:0.0.6") // TODO bring into monorepo
    {
        exclude(group = "com.google.inject", module = "guice")
    }

    implementation("org.apache.maven.resolver:maven-resolver-api:1.9.2")
    implementation("org.apache.maven.resolver:maven-resolver-spi:1.9.2")
    implementation("org.apache.maven.resolver:maven-resolver-util:1.9.2")
    implementation("org.apache.maven.resolver:maven-resolver-impl:1.9.2")
    implementation("org.apache.maven.resolver:maven-resolver-transport-file:1.9.2")
    implementation("org.apache.maven.resolver:maven-resolver-transport-http:1.9.2")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.2")
    implementation("org.apache.maven:maven-resolver-provider:3.8.6")
    implementation("org.apache.maven:maven-model-builder:3.8.6")

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.jgit)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
