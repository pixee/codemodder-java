plugins {
    id("io.codemodder.java-library")
    id("io.codemodder.maven-publish")
}

description = "Plugin for providing Maven dependency management functions to codemods."

dependencies {
    val commonsLangVersion = "3.12.0"
    val dom4jVersion = "2.1.3"
    val jaxenVersion = "1.2.0"
    val xercesImplVersion = "2.12.2"
    val xmlUnitVersion = "2.9.0"
    val kotlinVersion = "1.7.10"
    val javaSemverVersion = "0.9.0"
    val slf4jSimpleVersion = "2.0.0"
    val hamcrestVersion = "1.3"
    val junitVersion = "4.13.2"
    val kotlinTestVersion = "1.7.10"
    val slf4jApiVersion = "2.0.0"
    val juniversalchardetVersion = "2.4.0"
    val diffMatchPatchVersion = "0.0.2"

    compileOnly(libs.jetbrains.annotations)
    implementation(project(":framework:codemodder-base"))

    testImplementation(testlibs.bundles.junit.jupiter)
    testImplementation(testlibs.bundles.hamcrest)
    testImplementation(testlibs.assertj)
    testImplementation(testlibs.jgit)
    testImplementation(testlibs.mockito)
    testRuntimeOnly(testlibs.junit.jupiter.engine)

    implementation("org.apache.commons:commons-lang3:$commonsLangVersion")
    implementation("org.dom4j:dom4j:$dom4jVersion")
    implementation("jaxen:jaxen:$jaxenVersion")
    implementation("xerces:xercesImpl:$xercesImplVersion")
    implementation("org.xmlunit:xmlunit-core:$xmlUnitVersion")
    implementation("org.xmlunit:xmlunit-assertj3:$xmlUnitVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("com.github.zafarkhaja:java-semver:$javaSemverVersion")
    implementation("com.github.albfernandez:juniversalchardet:$juniversalchardetVersion")
    implementation("io.github.pixee:java-security-toolkit:1.0.7")
    testImplementation("fun.mike:diff-match-patch:$diffMatchPatchVersion")
    testImplementation("org.slf4j:slf4j-simple:$slf4jSimpleVersion")
    testImplementation("org.hamcrest:hamcrest-all:$hamcrestVersion")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinTestVersion")
    compileOnly("org.slf4j:slf4j-api:$slf4jApiVersion")
}
