rootProject.name = "codetl"

pluginManagement {
    includeBuild("gradle/settings")
}

plugins {
    id("io.openpixee.codetl.settings")
}

include("cli", "languages:java", "languages:javascript")
