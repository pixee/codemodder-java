package io.github.pixee.maven.operator

import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.FileModelSource
import org.apache.maven.model.building.ModelBuildingException
import org.apache.maven.project.ProjectModelResolver
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.DependencyVisitor
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.io.path.toPath

/**
 * This is a resolver that actually embeds much of Maven Logic into that.
 *
 * TODO: Support Profiles / Environment Variables
 * Support Third Party / User-Supplied Repositories (right now it only supports central)
 */
class QueryByResolver : AbstractSimpleQueryCommand() {
    private fun getLocalRepository(pm: ProjectModel): LocalRepository {
        val localRepositoryPath: File = getLocalRepositoryPath(pm)

        return LocalRepository(localRepositoryPath.absolutePath)
    }

    private fun newRepositorySystemSession(pm: ProjectModel, system: RepositorySystem): DefaultRepositorySystemSession? {
        val session = MavenRepositorySystemUtils.newSession()

        val localRepo = getLocalRepository(pm)

        session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)

        return session
    }


    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(QueryByResolver::class.java)
    }

    override fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModel) {
        TODO("Not yet implemented")
    }

    @Suppress("DEPRECATION")
    override fun execute(c: ProjectModel): Boolean {
        /*
         * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
         * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
         * factories.
         */
        val locator: org.eclipse.aether.impl.DefaultServiceLocator =
            MavenRepositorySystemUtils.newServiceLocator()
        locator.addService<org.eclipse.aether.spi.connector.RepositoryConnectorFactory>(
            org.eclipse.aether.spi.connector.RepositoryConnectorFactory::class.java,
            BasicRepositoryConnectorFactory::class.java
        )
        locator.addService<org.eclipse.aether.spi.connector.transport.TransporterFactory>(
            org.eclipse.aether.spi.connector.transport.TransporterFactory::class.java,
            FileTransporterFactory::class.java
        )
        locator.addService<org.eclipse.aether.spi.connector.transport.TransporterFactory>(
            org.eclipse.aether.spi.connector.transport.TransporterFactory::class.java,
            HttpTransporterFactory::class.java
        )

        locator.setErrorHandler(object :
            org.eclipse.aether.impl.DefaultServiceLocator.ErrorHandler() {
            override fun serviceCreationFailed(
                type: Class<*>?,
                impl: Class<*>?,
                exception: Throwable
            ) {
                LOGGER.error(
                    "Service creation failed for {} with implementation {}",
                    type, impl, exception
                )
            }
        })

        val remoteRepository =
            RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/")
                .build()

        val remoteRepositories = listOf(remoteRepository)

        val repositorySystem =
            locator.getService(org.eclipse.aether.RepositorySystem::class.java)

        val session = newRepositorySystemSession(c, repositorySystem)

        val modelBuilder = DefaultModelBuilderFactory().newInstance()

        val repositoryManager = DefaultRemoteRepositoryManager()

        val modelBuildingRequest = DefaultModelBuildingRequest().apply {
            val pomFile = c.pomPath!!.toURI().toPath().toFile()

            this.activeProfileIds = c.activeProfiles.filterNot { it.startsWith("!") }.toList()
            this.inactiveProfileIds =
                c.activeProfiles.filter { it.startsWith("!") }.map { it.substring(1) }.toList()

            this.userProperties = System.getProperties()
            this.systemProperties = System.getProperties()
            this.pomFile = pomFile

            this.isProcessPlugins = false

            this.modelSource = FileModelSource(pomFile)

            val modelResolver = ProjectModelResolver(
                session,
                null,
                repositorySystem,
                repositoryManager,
                remoteRepositories,
                null,
                null
            )

            this.modelResolver = modelResolver
        }

        val res = try {
            modelBuilder.build(modelBuildingRequest)
        } catch (e: ModelBuildingException) {
            LOGGER.warn("Oops: ", e)

            return false
        }

        val dependencyToArtifact: (org.apache.maven.model.Dependency) -> org.eclipse.aether.graph.Dependency =
            {
                org.eclipse.aether.graph.Dependency(
                    DefaultArtifact(
                        it.groupId,
                        it.artifactId,
                        it.classifier,
                        null,
                        it.version
                    ),
                    it.scope,
                )
            }

        val deps: List<org.eclipse.aether.graph.Dependency> =
            res.effectiveModel.dependencies?.map(dependencyToArtifact)?.toList() ?: emptyList()

        val managedDeps: List<org.eclipse.aether.graph.Dependency> =
            res.effectiveModel.dependencyManagement?.dependencies?.map(dependencyToArtifact)
                ?.toList() ?: emptyList()

        val collectRequest = CollectRequest(deps, managedDeps, remoteRepositories)

        val collectResult = repositorySystem.collectDependencies(session, collectRequest)

        val returnList: MutableList<Dependency> = mutableListOf()

        collectResult.root.accept(object : DependencyVisitor {
            override fun visitEnter(node: DependencyNode?): Boolean {
                node?.dependency?.apply {
                    returnList.add(
                        Dependency(
                            groupId = this.artifact.groupId,
                            artifactId = this.artifact.artifactId,
                            version = this.artifact.version,
                            classifier = this.artifact.classifier,
                            packaging = this.artifact.extension,
                            scope = this.scope,
                        )
                    )
                }

                return true
            }

            override fun visitLeave(node: DependencyNode?): Boolean {
                return true
            }
        })

        this.result = returnList.toList()

        return true
    }

}