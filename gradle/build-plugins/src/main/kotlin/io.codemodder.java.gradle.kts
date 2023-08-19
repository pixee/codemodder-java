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

tasks.withType(Test::class) {
    useJUnitPlatform()
}
