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
    implementation(buildlibs.jib)
    implementation(buildlibs.fileversioning)
    implementation(buildlibs.spotless)
    implementation(buildlibs.nebula.publish.plugin)
    implementation(buildlibs.nebula.contacts.plugin)
}
