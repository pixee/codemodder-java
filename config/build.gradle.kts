plugins {
    id("io.openpixee.codetl.java-library")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
