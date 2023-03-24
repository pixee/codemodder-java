rootProject.name = "codetl"

pluginManagement {
    includeBuild("gradle/settings")
}

plugins {
    id("io.openpixee.codetl.settings")
}

include("cli", "languages:java", "languages:javascript", "languages:codemodder-framework-java", "languages:codemodder-semgrep-provider", "languages:codemodder-default-codemods", "languages:codemodder-common", "languages:codemodder-testutils")
