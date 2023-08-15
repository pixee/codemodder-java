plugins {
    alias(buildlibs.plugins.fileversioning)
    alias(buildlibs.plugins.nexus.publish)
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}


/*
 In a composite build, running lifecycle tasks (such as check) from the root will not automatically
 propagate to subprojects. See https://github.com/gradle/gradle/issues/20863
*/
val lifecycleTasks = listOf(
    LifecycleBasePlugin.ASSEMBLE_TASK_NAME,
    LifecycleBasePlugin.BUILD_TASK_NAME,
    LifecycleBasePlugin.CHECK_TASK_NAME,
    LifecycleBasePlugin.CLEAN_TASK_NAME,
    "spotlessApply"
)
for (task in lifecycleTasks) {
    tasks.register(task) {
        group = "lifecycle"
        description = "Runs the $task task for all included builds."
        val tasks = gradle.includedBuilds.map {it.task(":$task") }
        dependsOn(tasks)
    }
}

tasks.register(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) {
    group = "lifecycle"
    description = "Runs the ${PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME} task for all included builds."
    val tasks =
        gradle.includedBuilds.filter {
            // filter out the gradle build logic builds, because we never publish artifacts from there
            !it.projectDir.parentFile.name.equals("gradle")
        }.map {
            it.task(":${PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME}")
        }
    dependsOn(tasks)
}

tasks.register(MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME) {
    group = "lifecycle"
    description = "Runs the ${MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME} task for all included builds."
    val tasks =
            gradle.includedBuilds.filter {
                // filter out the gradle build logic builds, because we never publish artifacts from there
                !it.projectDir.parentFile.name.equals("gradle")
            }.map {
                it.task(":${MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME}")
            }
    dependsOn(tasks)
}
