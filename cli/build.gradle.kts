plugins {
    id("io.openpixee.codetl.java")
    id("io.openpixee.codetl.native-image")
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
    implementation(libs.picocli)

    add(bundle.name, project(":languages:javascript", "bundle"))
}

val copyBundle by tasks.registering(Copy::class) {
    from(bundle)
    into(layout.buildDirectory.dir("generated/sources/$name/main/resources"))
}

sourceSets.main {
    resources.srcDir(copyBundle)
}