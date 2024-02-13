plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.runner")
    id("io.codemodder.maven-publish")
    id("io.codemodder.core-codemods-docs")
    `jvm-test-suite`
}

val main = "io.codemodder.codemods.DefaultCodemods"

description = "Codemods for fixing common errors across many Java projects"

application {
    mainClass.set(main)
}

dependencies {
    implementation(project(":framework:codemodder-base"))
    implementation(project(":plugins:codemodder-plugin-semgrep"))
    implementation(project(":plugins:codemodder-plugin-codeql"))
    implementation(project(":plugins:codemodder-plugin-maven"))
    implementation(project(":plugins:codemodder-plugin-llm"))
    implementation(project(":plugins:codemodder-plugin-aws"))
    implementation(project(":plugins:codemodder-plugin-pmd"))
    implementation(project(":plugins:codemodder-plugin-sonar"))
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
    testImplementation("org.testcontainers:testcontainers:1.19.0")
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

val getTestProjectNames by tasks.registering {
    group = "custom"
    description = "Extracts codemodIds from IntegrationTestMetadata"

    val sourceDir = file("src/test/java/io/codemodder/codemods/integration/tests")
    val codemodIdRegex = """codemodId\s*=\s*["']([^"']+)["']""".toRegex()

    doLast {
        val codemodIds = mutableListOf<String>()
        sourceDir
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith(".java") }
            .forEach { file ->
                val content = file.readText()
                val matchResult = codemodIdRegex.find(content)

                if (matchResult != null) {
                    codemodIds.add(matchResult.groupValues[1])
                }
            }

        println(codemodIds.joinToString(" "))
    }
}

tasks.withType(JavaCompile::class) {
    dependsOn(getTestProjectNames)
}
