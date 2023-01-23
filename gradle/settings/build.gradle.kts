@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
    `kotlin-dsl`
    alias(buildlibs.plugins.spotless)
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "src/main/kotlin/*.gradle.kts")
        ktlint()
    }
}

dependencies {
    implementation(buildlibs.spotless)
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}
