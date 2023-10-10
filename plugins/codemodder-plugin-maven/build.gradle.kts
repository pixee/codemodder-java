plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

description = "Plugin for providing Maven dependency management functions to codemods."

dependencies {
    val docoptVersion = "0.6.0.20150202"
    val commonsLangVersion = "3.12.0"
    val dom4jVersion = "2.1.3"
    val jaxenVersion = "1.2.0"
    val xercesImplVersion = "2.12.2"
    val xmlUnitVersion = "2.9.0"
    val kotlinVersion = "1.7.10"
    val javaSemverVersion = "0.9.0"
    val commonsIoVersion = "2.11.0"
    val mavenInvokerVersion = "3.2.0"
    val mavenEmbedderVersion = "3.8.6"
    val guiceVersion = "5.1.0"
    val mavenResolverVersion = "1.9.2"
    val mavenProviderVersion = "3.8.6"
    val modelBuilderVersion = "3.8.6"
    val juniversalchardetVersion = "2.4.0"
    val commonsCollections4Version = "4.1"
    val slf4jSimpleVersion = "2.0.0"
    val diffMatchPatchVersion = "0.0.2"
    val javaDiffUtilsVersion = "4.12"
    val hamcrestVersion = "1.3"
    val junitVersion = "4.13.2"
    val kotlinTestVersion = "1.7.10"
    val slf4jApiVersion = "2.0.0"
    val lombokVersion = "1.18.30"

    compileOnly(libs.jetbrains.annotations)
    implementation(project(":framework:codemodder-base"))

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.jgit)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)

    implementation("com.offbytwo:docopt:$docoptVersion")
    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")
    implementation("org.dom4j:dom4j:$dom4jVersion")
    implementation("jaxen:jaxen:$jaxenVersion")
    implementation("xerces:xercesImpl:$xercesImplVersion")
    implementation("org.xmlunit:xmlunit-core:$xmlUnitVersion")
    implementation("org.xmlunit:xmlunit-assertj3:$xmlUnitVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("com.github.zafarkhaja:java-semver:$javaSemverVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.apache.maven.shared:maven-invoker:$mavenInvokerVersion")
    implementation("org.apache.maven:maven-embedder:$mavenEmbedderVersion") {
        exclude(group = "com.google.inject", module = "guice")
    }
    implementation("org.apache.maven:maven-compat:$mavenEmbedderVersion") {
        exclude(group = "com.google.inject", module = "guice")
    }
    implementation("com.google.inject:guice:$guiceVersion")
    implementation("org.apache.maven.resolver:maven-resolver-api:$mavenResolverVersion")
    implementation("org.apache.maven.resolver:maven-resolver-spi:$mavenResolverVersion")
    implementation("org.apache.maven.resolver:maven-resolver-util:$mavenResolverVersion")
    implementation("org.apache.maven.resolver:maven-resolver-impl:$mavenResolverVersion")
    implementation("org.apache.maven.resolver:maven-resolver-transport-file:$mavenResolverVersion")
    implementation("org.apache.maven.resolver:maven-resolver-transport-http:$mavenResolverVersion")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:$mavenResolverVersion")
    implementation("org.apache.maven:maven-resolver-provider:$mavenProviderVersion")
    implementation("org.apache.maven:maven-model-builder:$modelBuilderVersion")
    implementation("com.github.albfernandez:juniversalchardet:$juniversalchardetVersion")
    implementation("org.apache.commons:commons-collections4:$commonsCollections4Version")
    testImplementation("org.slf4j:slf4j-simple:$slf4jSimpleVersion")
    testImplementation("fun.mike:diff-match-patch:$diffMatchPatchVersion")
    testImplementation("io.github.java-diff-utils:java-diff-utils:$javaDiffUtilsVersion")
    testImplementation("org.hamcrest:hamcrest-all:$hamcrestVersion")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinTestVersion")
    compileOnly("org.slf4j:slf4j-api:$slf4jApiVersion")
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
}
