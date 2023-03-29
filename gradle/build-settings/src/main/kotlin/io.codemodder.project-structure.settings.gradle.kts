// this plugin gives us one place to modify the strategy we use to structure subprojects

// this strategy automatically includes all projects that are direct children of the root directory
rootDir.listFiles()?.filter { File(it, "build.gradle.kts").exists() }?.forEach { subproject ->
    include(subproject.name)
}
