rootProject.name = "codetl"

pluginManagement {
    includeBuild("gradle/settings")
}

plugins {
    id("io.openpixee.codetl.settings")
}

include("cli", "config", "languages:java", "languages:java-codemod", "languages:javascript")
