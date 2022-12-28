plugins {
    id("io.openpixee.codetl.java")
    id("io.openpixee.codetl.native-image")
}

application {
    mainClass.set("io.openpixee.codetl.cli.Application")
}

dependencies {
    implementation(libs.picocli)

    annotationProcessor(libs.picocli.codegen)
}

