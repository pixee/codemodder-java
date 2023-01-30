@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
    id("io.openpixee.codetl.application")
    id("io.openpixee.codetl.native-image")
    alias(libs.plugins.jib)
}

java {
    toolchain {
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

application {
    mainClass.set("io.openpixee.codetl.cli.Application")
}

jib.to.image = "codetl"

graalvmNative {
    binaries {
        named("main") {
            imageName.set("codetl")
            buildArgs("--language:js")
            resources {
                includedPatterns.add(".*/javascript-language-provider.js$")
            }
        }
    }
}

testing {
    suites {
        getByName("test", JvmTestSuite::class) {
            dependencies {
                implementation.bundle(testlibs.bundles.junit.jupiter)
                implementation.bundle(testlibs.bundles.hamcrest)
                runtimeOnly(testlibs.junit.jupiter.engine)
            }
        }
        getByName("integrationTest", JvmTestSuite::class) {
            dependencies {
                implementation.bundle(testlibs.bundles.hamcrest)
                implementation.bundle(testlibs.bundles.junit.jupiter)
                implementation("io.github.pixee:codetf-java:0.0.2") // TODO bring codetf-java into the monorepo)
                implementation(libs.picocli)
                implementation(testlibs.jgit)
            }
        }
    }
}

val bundle by configurations.registering {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    compileOnly(libs.graal.sdk)
    annotationProcessor(libs.picocli.codegen)

    implementation(libs.commons.lang3)
    implementation(libs.jfiglet)
    implementation(libs.logback.classic)
    implementation(libs.picocli)
    implementation(libs.slf4j.api)
    implementation(project(":config"))
    implementation(project(":languages:java"))

    add(bundle.name, project(":languages:javascript", "bundle"))
}

val extractApplicationDistribution by tasks.registering(Copy::class) {
    from(zipTree(tasks.distZip.flatMap { it.archiveFile }))
    into(layout.buildDirectory.dir("application"))
}

tasks.integrationTest {
    // TODO not yet using native image, because we still use too many incompatible dependencies
//    dependsOn(tasks.nativeCompile)
//    val executable = tasks.nativeCompile.flatMap { it.outputFile }.map { it.asFile.path }
    // TODO use application plugin distribution in integration tests, until native image challenges resolved
    dependsOn(extractApplicationDistribution)
    val executable = layout.buildDirectory.file("application/codetl/bin/codetl")
    doFirst {
        systemProperty(
            "io.openpixee.codetl.test.executable",
            executable.get()
        )
    }
}

val copyBundle by tasks.registering(Copy::class) {
    from(bundle)
    into(layout.buildDirectory.dir("generated/sources/$name/main/resources"))
}

sourceSets.main {
    resources.srcDir(copyBundle)
}
