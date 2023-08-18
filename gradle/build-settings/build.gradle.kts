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
