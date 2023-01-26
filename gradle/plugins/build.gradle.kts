plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless")
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "src/main/kotlin/*.gradle.kts")
        ktlint()
    }
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}

dependencies {
    implementation(buildlibs.spotless)
    implementation(buildlibs.graalvm.nativeImage)
}
