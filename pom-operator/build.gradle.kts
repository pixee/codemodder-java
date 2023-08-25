plugins {
    id("io.codemodder.kotlin")
}

dependencies {
    api("com.offbytwo:docopt:0.6.0.20150202")
    api("org.apache.commons:commons-lang3:3.12.0")
    api("org.dom4j:dom4j:2.1.3")
    api("jaxen:jaxen:1.2.0")
    api("xerces:xercesImpl:2.12.2")
    api("org.xmlunit:xmlunit-core:2.9.0")
    api("org.xmlunit:xmlunit-assertj3:2.9.0")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.10")
    api("com.github.zafarkhaja:java-semver:0.9.0")
    api("commons-io:commons-io:2.11.0")
    api("org.apache.maven.shared:maven-invoker:3.2.0")
    api("com.google.inject:guice:5.1.0")
    api("org.apache.maven:maven-embedder:3.8.6") {
        exclude(group = "com.google.inject", module = "guice")
    }
    api("org.apache.maven:maven-compat:3.8.6") {
        exclude(group = "com.google.inject", module = "guice")
    }
    api("org.apache.maven.resolver:maven-resolver-api:1.9.2")
    api("org.apache.maven.resolver:maven-resolver-spi:1.9.2")
    api("org.apache.maven.resolver:maven-resolver-util:1.9.2")
    api("org.apache.maven.resolver:maven-resolver-impl:1.9.2")
    api("org.apache.maven.resolver:maven-resolver-transport-file:1.9.2")
    api("org.apache.maven.resolver:maven-resolver-transport-http:1.9.2")
    api("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.2")
    api("org.apache.maven:maven-resolver-provider:3.8.6")
    api("org.apache.maven:maven-model-builder:3.8.6")
    api("com.github.albfernandez:juniversalchardet:2.4.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.0")
    testImplementation("fun.mike:diff-match-patch:0.0.2")
    testImplementation("io.github.java-diff-utils:java-diff-utils:4.12")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation(testlibs.bundles.junit.jupiter)
    compileOnly("org.slf4j:slf4j-api:2.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
