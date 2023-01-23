plugins {
    id("io.openpixee.codetl.base")
    id("io.openpixee.codetl.java")
    id("io.openpixee.codetl.native-image")
}

java {
    toolchain {
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

application {
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

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testRuntimeOnly(testlibs.junit.jupiter.engine)

    integrationTestImplementation(libs.picocli)
    integrationTestImplementation("io.github.pixee:codetf-java:0.0.2") // TODO bring codetf-java into the monorepo)
    integrationTestImplementation(testlibs.bundles.junit.jupiter)
    integrationTestImplementation(testlibs.jgit)
    integrationTestImplementation(testlibs.bundles.hamcrest)
}

val copyBundle by tasks.registering(Copy::class) {
    from(bundle)
    into(layout.buildDirectory.dir("generated/sources/$name/main/resources"))
}

sourceSets.main {
    resources.srcDir(copyBundle)
}
