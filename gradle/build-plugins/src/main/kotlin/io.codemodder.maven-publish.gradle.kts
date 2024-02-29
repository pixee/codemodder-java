plugins {
    id("io.codemodder.spotless")
    id("de.epitschke.gradle-file-versioning")
    `maven-publish`
    signing
    id("com.netflix.nebula.contacts")
    id("com.netflix.nebula.source-jar")
    id("com.netflix.nebula.javadoc-jar")
    id("com.netflix.nebula.maven-publish")
    id("com.netflix.nebula.publish-verification")
}

extensions.getByType<nebula.plugin.contacts.ContactsExtension>().run {
    addPerson(
        "support@pixee.ai",
        delegateClosureOf<nebula.plugin.contacts.Contact> {
            moniker("Pixee")
            github("pixee")
        },
    )
}

val publicationName = "nebula"
signing {
    if (providers.environmentVariable("CI").isPresent) {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(extensions.getByType<PublishingExtension>().publications.getByName(publicationName))
}

publishing {
    publications {
        named<MavenPublication>(publicationName) {
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                if (!pluginManager.hasPlugin("com.netflix.nebula.maven-apache-license")) {
                    licenses {
                        license {
                            name.set("GNU AFFERO GENERAL PUBLIC LICENSE, Version 3 (AGPL-3.0)")
                            url.set("https://opensource.org/licenses/AGPL-3.0")
                        }
                    }
                }
                val scmHost = "github.com"
                val scmProject = "pixee/codemodder-java"
                val projectUrl = "https://$scmHost/$scmProject"
                url.set(projectUrl)
                scm {
                    url.set(projectUrl)
                    connection.set("scm:git:git@$scmHost:$scmProject")
                    developerConnection.set(connection)
                }
            }
        }
    }
}
