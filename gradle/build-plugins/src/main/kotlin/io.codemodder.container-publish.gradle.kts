plugins {
    id("io.codemodder.runner")
    id("com.google.cloud.tools.jib")
}

jib.from.image = "iulspop/special-base-image"
