plugins {
    alias(buildlibs.plugins.fileversioning)
    alias(buildlibs.plugins.nexus.publish)
    id("io.codemodder.base")
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
