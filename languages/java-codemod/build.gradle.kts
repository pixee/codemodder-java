plugins {
    id("io.openpixee.codetl.base")
    id("io.openpixee.codetl.java-library")
}

dependencies {
    implementation(libs.contrast.sarif)
    implementation(libs.spoon.core)
}

testing {
    suites {
        named("test", JvmTestSuite::class) {
            dependencies {
                implementation(testlibs.assertj)
                implementation.bundle(testlibs.bundles.junit.jupiter)
                runtimeOnly(testlibs.junit.jupiter.engine)
            }
        }
    }
}
