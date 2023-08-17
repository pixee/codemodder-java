plugins {
    id("io.codemodder.root")
    id("io.codemodder.java-library")
    id("io.codemodder.runner")
    id("io.codemodder.container-publish")
    id("io.codemodder.maven-publish")
    `jvm-test-suite`
}

val main = "io.codemodder.codemods.DefaultCodemods"

description = "Codemods for fixing common errors across many Java projects"

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
    testImplementation("io.codemodder:codemodder-testutils-llm")
    testRuntimeOnly(testlibs.junit.jupiter.engine)
    testImplementation(testlibs.jgit)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)
        register<JvmTestSuite>("intTest") {
            testType.set(TestSuiteType.INTEGRATION_TEST)
            dependencies {
                implementation(project())
                implementation("io.codemodder:codemodder-testutils")
                implementation.bundle(testlibs.bundles.junit.jupiter)
                implementation.bundle(testlibs.bundles.hamcrest)
                implementation(testlibs.jgit)
                implementation(libs.juniversalchardet)
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("intTest"))
}
