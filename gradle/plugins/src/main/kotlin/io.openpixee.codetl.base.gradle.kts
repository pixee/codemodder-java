plugins {
    id("com.diffplug.spotless")
}

group = "io.openpixee.codetl"

spotless {
    kotlinGradle {
        ktlint()
    }
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}
