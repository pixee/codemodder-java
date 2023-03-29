plugins {
    id("com.diffplug.spotless")
}

group = "io.codemodder"

spotless {
    kotlinGradle {
        ktlint()
    }
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}
