plugins {
    `kotlin-dsl`
    id("io.openpixee.codetl.script-formatting")
}

group = "io.openpixee.codetl.buildlogic"

dependencies {
    implementation(buildlibs.spotless)
    implementation(buildlibs.graalvm.nativeImage)
}
