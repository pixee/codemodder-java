plugins {
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
                        val openAIKey = providers.gradleProperty("codemodderOpenAIKey")
                        inputs.property("codemodderOpenAIKey", openAIKey)
                        if (openAIKey.isPresent) {
                            environment("CODEMODDER_OPENAI_API_KEY", openAIKey.get())
                        }
                    }
                }
            }
        }
    }
}
