@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
    `kotlin-dsl`
    alias(buildlibs.plugins.spotless)
}

group = "io.openpixee.codetl.buildlogic"

spotless {
    kotlinGradle {
        ktlint()
    }
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}
