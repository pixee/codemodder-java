import com.diffplug.spotless.LineEnding

plugins {
    `kotlin-dsl`
    alias(buildlibs.plugins.spotless)
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "src/main/kotlin/*.gradle.kts")
        ktlint()
    }
    // https://github.com/diffplug/spotless/issues/1644
    lineEndings = LineEnding.PLATFORM_NATIVE
}

dependencies {
    implementation(buildlibs.spotless)
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}
