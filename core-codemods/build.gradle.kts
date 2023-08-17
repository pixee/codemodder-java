plugins {
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
    implementation(project(":framework:codemodder-base"))
    implementation(project(":plugins:codemodder-plugin-semgrep"))
    implementation(project(":plugins:codemodder-plugin-codeql"))
    implementation(project(":plugins:codemodder-plugin-maven"))
    implementation(project(":plugins:codemodder-plugin-llm"))
    implementation(project(":plugins:codemodder-plugin-aws"))
    implementation(project(":plugins:codemodder-plugin-pmd"))
    implementation(libs.juniversalchardet)
    implementation(libs.dom4j)
    implementation(libs.commons.jexl)
    implementation(libs.tuples)
    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.mockito)
    testImplementation(project(":framework:codemodder-testutils"))
    testImplementation(project(":framework:codemodder-testutils-llm"))
    testRuntimeOnly(testlibs.junit.jupiter.engine)
    testImplementation(testlibs.jgit)
}

val integrationTestSuiteName = "intTest"
testing {
    @Suppress("UnstableApiUsage")
    suites {
        val test by getting(JvmTestSuite::class)
        register<JvmTestSuite>(integrationTestSuiteName) {
            testType.set(TestSuiteType.INTEGRATION_TEST)
            dependencies {
                implementation(project())
                implementation(project(":framework:codemodder-testutils"))
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
    @Suppress("UnstableApiUsage")
    dependsOn(testing.suites.named(integrationTestSuiteName))
}
