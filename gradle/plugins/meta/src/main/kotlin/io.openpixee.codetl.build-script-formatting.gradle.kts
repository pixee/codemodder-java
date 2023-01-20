plugins {
    base
    id("com.diffplug.spotless")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}
