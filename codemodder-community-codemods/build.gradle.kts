plugins {
    id("io.codemodder.root")
    id("io.codemodder.java-library")
    id("io.codemodder.runner")
    id("io.codemodder.container-publish")
    id("io.codemodder.maven-publish")
}

application {
    mainClass.set("io.codemodder.codemods.Runner")
}

jib.to.image = "218200003247.dkr.ecr.us-east-1.amazonaws.com/pixee/codemodder-java:${project.version}"
tasks.publish {
    dependsOn(tasks.jib)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation("io.codemodder:codemodder-core")
    implementation("io.codemodder:codemodder-plugin-semgrep")

    implementation(libs.dom4j)
    implementation("io.openpixee:java-jdbc-parameterizer:0.0.8") // TODO bring into monorepo

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)
    testImplementation("io.codemodder:codemodder-testutils")
    testRuntimeOnly(testlibs.junit.jupiter.engine)
}
