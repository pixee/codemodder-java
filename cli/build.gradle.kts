plugins {
    id("io.openpixee.codetl.application")
    id("io.openpixee.codetl.native-image")
}

java {
    toolchain {
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

application {
    applicationName = "codetl"
    mainClass.set("io.openpixee.codetl.cli.Application")
}

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

    implementation(libs.picocli)
    implementation(libs.slf4j.api)
    implementation(project(":config"))
    implementation(project(":languages:java"))

    add(bundle.name, project(":languages:javascript", "bundle"))
}

tasks.integrationTest {
    dependsOn(tasks.nativeCompile)
    val executable = tasks.nativeCompile.flatMap { it.outputFile }.map { it.asFile.path }
    doFirst {
//        systemProperty("io.openpixee.codetl.test.executable", executable.get())
        systemProperty("io.openpixee.codetl.test.executable", layout.buildDirectory.file("distributions/codetl/bin/codetl").get())
    }
}

val copyBundle by tasks.registering(Copy::class) {
    from(bundle)
    into(layout.buildDirectory.dir("generated/sources/$name/main/resources"))
}

sourceSets.main {
    resources.srcDir(copyBundle)
}
