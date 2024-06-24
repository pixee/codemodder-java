plugins {
    id("io.codemodder.java")
    `jvm-test-suite`
}

testing {
    suites {
        @Suppress("UnstableApiUsage")
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            targets {
                all {
                    testTask {
                        val openAIKey = project.findProperty("codemodderOpenAIKey") as String?
                        openAIKey?.let {
                            environment("CODEMODDER_OPENAI_API_KEY", it)
                        }
                    }
                }
            }
        }
    }
}
