plugins {
    `kotlin-dsl`
    id("com.diffplug.spotless")
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "src/main/kotlin/*.gradle.kts")
        ktlint()
    }
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}

dependencies {
    implementation(buildlibs.jib)
    implementation(buildlibs.fileversioning)
    implementation(buildlibs.spotless)
    implementation(buildlibs.nebula.publish.plugin)
    implementation(buildlibs.nebula.contacts.plugin)
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
    tasks.named(task) {
        dependsOn(subprojects.map { ":${it.name}:$task" })
    }
}
