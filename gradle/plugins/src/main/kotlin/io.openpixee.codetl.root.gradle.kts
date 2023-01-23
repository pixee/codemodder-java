/* This root plugin is only for the root of the multi-component build. Despite only being for one project, we define the build logic for that build script in this plugin, because doing so allows us to encapsulate the messy details of how the gradle build logic is structured.  */

plugins {
    id("io.openpixee.codetl.base")
}

// running check at the root will not automatically run check on builds included via pluginManagement; however, we need it to run, so we can catch errors in the plugins projects.
tasks.check {
    gradle.includedBuilds.map { it.task(":" + LifecycleBasePlugin.CHECK_TASK_NAME) }
}

tasks.spotlessApply {
    dependsOn(gradle.includedBuilds.map { it.task(":spotlessApply") })
    dependsOn(gradle.includedBuild("meta-plugins").task(":settings:spotlessApply"))
    dependsOn(gradle.includedBuild("meta-plugins").task(":plugin-analysis-plugins:spotlessApply"))
}
