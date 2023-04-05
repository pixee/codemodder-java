rootProject.name = "plugins"

pluginManagement {
    includeBuild("../build-logic")
}

plugins {
    id("io.codemodder.settings")
}
