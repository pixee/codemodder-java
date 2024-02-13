import com.diffplug.spotless.LineEnding

plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless")
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "src/main/kotlin/*.gradle.kts")
        ktlint()
    }
    // https://github.com/diffplug/spotless/issues/1644
    lineEndings = LineEnding.PLATFORM_NATIVE
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}

dependencies {
    implementation(libs.javaparser.core)
    implementation(libs.gson)
    implementation(buildlibs.fileversioning)
    implementation(buildlibs.spotless)
    implementation(buildlibs.nebula.publish.plugin)
    implementation(buildlibs.nebula.contacts.plugin)
}
