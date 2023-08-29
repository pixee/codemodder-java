import com.diffplug.spotless.LineEnding

plugins {
    id("io.codemodder.base")
    id("com.diffplug.spotless")
}

spotless {
    kotlinGradle {
        ktlint()
    }
    // https://github.com/diffplug/spotless/issues/1644
    lineEndings = LineEnding.PLATFORM_NATIVE
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}
