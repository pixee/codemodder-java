import org.gradle.kotlin.dsl.version

plugins {
    id("io.codemodder.spotless")
    id("org.jetbrains.kotlin.jvm")
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

tasks.withType(Test::class) {
    useJUnitPlatform()
}
