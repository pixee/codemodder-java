plugins {
    id("io.codemodder.base")
    id("com.diffplug.spotless")
}

spotless {
    kotlinGradle {
        ktlint()
    }
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}
