plugins {
    id("io.openpixee.codetl.base")
    id("com.diffplug.spotless")
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
