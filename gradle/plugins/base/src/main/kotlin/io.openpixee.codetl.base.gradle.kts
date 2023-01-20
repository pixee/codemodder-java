plugins {
    base
    id("com.diffplug.spotless")
}

group = "io.openpixee.codetl"

spotless {
    kotlinGradle {
        ktlint()
    }
}
