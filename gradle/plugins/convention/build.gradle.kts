plugins {
    `kotlin-dsl`
}

group = "io.openpixee.codetl.buildlogic"

dependencies {
    implementation(buildlibs.spotless)
    implementation(buildlibs.graalvm.nativeImage)
}