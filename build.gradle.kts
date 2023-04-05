@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
    alias(buildlibs.plugins.fileversioning)
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
        dependsOn(gradle.includedBuilds.map { it.task(":$task") })
    }
}
