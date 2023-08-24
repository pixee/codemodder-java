package io.github.pixee.maven.operator

import org.apache.maven.model.building.*
import org.apache.maven.project.ProjectModelResolver
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

data class EmbedderFacadeRequest(
    val offline: Boolean,
    val localRepositoryPath: File? = null,
    val pomFile: File?,
    val activeProfileIds: Collection<String> = emptyList(),
    val inactiveProfileIds: Collection<String> = emptyList(),
)

data class EmbedderFacadeResponse(
    val modelBuildingResult: ModelBuildingResult,
    val session: DefaultRepositorySystemSession?,
    val repositorySystem: RepositorySystem?,
    val remoteRepositories: List<RemoteRepository> = emptyList(),
)

object EmbedderFacade {
    // Embedder Impl

    fun invokeEmbedder(req: EmbedderFacadeRequest): EmbedderFacadeResponse {
        val localRepoPath: File = when {
            req.localRepositoryPath != null -> req.localRepositoryPath
            System.getenv("M2_REPO") != null -> File(System.getenv("M2_REPO"))
            System.getProperty("maven.repo.local") != null -> File(System.getProperty("maven.repo.local"))
            else -> File(
                System.getProperty("user.home"),
                ".m2/repository"
            )
        }

        val localRepository = LocalRepository(localRepoPath.absolutePath)

        val locator: DefaultServiceLocator =
            MavenRepositorySystemUtils.newServiceLocator()

        locator.addService(
            RepositoryConnectorFactory::class.java,
            BasicRepositoryConnectorFactory::class.java
        )

        locator.addService(
            TransporterFactory::class.java,
            FileTransporterFactory::class.java
        )

        locator.addService(
            TransporterFactory::class.java,
            HttpTransporterFactory::class.java
        )

        locator.setErrorHandler(object :
            DefaultServiceLocator.ErrorHandler() {
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

        val repositorySystem = locator.getService(RepositorySystem::class.java)

        val session = MavenRepositorySystemUtils.newSession()

        session.localRepositoryManager =
            repositorySystem.newLocalRepositoryManager(session, localRepository)

        session.setOffline(req.offline)

        val modelBuilder = DefaultModelBuilderFactory().newInstance()

        val repositoryManager = DefaultRemoteRepositoryManager()

        val remoteRepository =
            RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/")
                .build()

        val remoteRepositories = if (req.offline) {
            emptyList()
        } else {
            listOf(remoteRepository)
        }

        val modelBuildingRequest = DefaultModelBuildingRequest().apply {
            this.userProperties = System.getProperties()
            this.systemProperties = System.getProperties()
            this.pomFile = req.pomFile!!

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

        val modelBuildingResult: ModelBuildingResult = try {
            modelBuilder.build(modelBuildingRequest)
        } catch (e: ModelBuildingException) {
            LOGGER.warn("Oops: ", e)

            throw e
        }

        return EmbedderFacadeResponse(
            modelBuildingResult = modelBuildingResult,
            session = session,
            repositorySystem = repositorySystem,
            remoteRepositories = remoteRepositories,
        )
    }

    private val LOGGER: Logger = LoggerFactory.getLogger(EmbedderFacade::class.java)
}