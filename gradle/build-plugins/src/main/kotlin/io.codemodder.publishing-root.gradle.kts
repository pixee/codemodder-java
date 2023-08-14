/*
 In a composite build, tasks run from the root will not automatically propagate to subprojects (see
 https://github.com/gradle/gradle/issues/20863).

 This plugin is for root build scripts that do not themselves have a "publish" task. It adds a
 "publish" task that depends on the "publish" tasks of all subprojects, to emulate typical Gradle
 behavior.
*/
tasks.register(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    description = "Publishes all publications produced by subprojects."
    dependsOn(subprojects.map { ":${it.name}:${PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME}" })
}

tasks.register(MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME) {
    group = PublishingPlugin.PUBLISH_TASK_GROUP
    description = "Publishes all publications produced by subprojects."
    dependsOn(subprojects.map { ":${it.name}:${MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME}" })
}
