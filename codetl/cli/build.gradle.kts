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
        }
    }
}

dependencies {
    compileOnly(libs.graal.sdk)
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)
}
