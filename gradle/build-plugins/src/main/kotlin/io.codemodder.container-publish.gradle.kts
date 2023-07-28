plugins {
    id("io.codemodder.runner")
    id("com.google.cloud.tools.jib")
}

jib.from.image = "218200003247.dkr.ecr.us-east-1.amazonaws.com/pixee/codemodder-java:base-image"
