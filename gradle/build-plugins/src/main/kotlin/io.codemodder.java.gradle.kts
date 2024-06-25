plugins {
    id("io.codemodder.spotless")
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

spotless {
    java {
        googleJavaFormat()
    }
}

testing {
    @Suppress("UnstableApiUsage")
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            targets {
                all {
                    testTask.configure {
                        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
                        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
                    }
                }
            }
        }
    }
}
