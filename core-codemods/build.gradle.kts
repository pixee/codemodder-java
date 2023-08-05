plugins {
    id("io.codemodder.root")
    id("io.codemodder.java-library")
    id("io.codemodder.runner")
    id("io.codemodder.container-publish")
    id("io.codemodder.maven-publish")
}

val main = "io.codemodder.codemods.DefaultCodemods"

application {
    mainClass.set(main)
}

jib {
    container {
        mainClass = main
    }
    to {
        image = "218200003247.dkr.ecr.us-east-1.amazonaws.com/pixee/codemodder-java"
        tags = setOf(de.epitschke.gradle.fileversioning.FileVersioningPlugin.getVersionFromFile(), "latest")
    }
}

tasks.publish {
    dependsOn(tasks.jib)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("io.codemodder:codemodder-base")
    implementation("io.codemodder:codemodder-plugin-semgrep")
    implementation("io.codemodder:codemodder-plugin-codeql")
    implementation("io.codemodder:codemodder-plugin-maven")
    implementation("io.codemodder:codemodder-plugin-llm")
    implementation("io.codemodder:codemodder-plugin-aws")
    implementation("io.codemodder:codemodder-plugin-pmd")
    implementation(libs.juniversalchardet)
    implementation(libs.dom4j)
    implementation(libs.commons.jexl)
    implementation(libs.tuples)
    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)
    testImplementation("io.codemodder:codemodder-testutils")
    testRuntimeOnly(testlibs.junit.jupiter.engine)
    testImplementation(testlibs.jgit)
}

sourceSets {
    create("intTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

val intTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val intTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

val integrationTest = task<Test>("intTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath
    shouldRunAfter("test")
}

tasks.check { dependsOn(integrationTest) }
