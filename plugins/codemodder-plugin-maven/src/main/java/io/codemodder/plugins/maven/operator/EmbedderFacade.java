package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.maven.model.building.*;
import org.apache.maven.project.ProjectModelResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRemoteRepositoryManager;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EmbedderFacade {

  private static EmbedderFacade instance = new EmbedderFacade();
  private static final Logger LOGGER = LoggerFactory.getLogger(EmbedderFacade.class);

  private EmbedderFacade() {}

  public static EmbedderFacadeResponse invokeEmbedder(EmbedderFacadeRequest req)
      throws ModelBuildingException {
    File localRepoPath;

    if (req.getLocalRepositoryPath() != null) {
      localRepoPath = req.getLocalRepositoryPath();
    } else if (System.getenv("M2_REPO") != null) {
      localRepoPath = new File(System.getenv("M2_REPO"));
    } else if (System.getProperty("maven.repo.local") != null) {
      localRepoPath = new File(System.getProperty("maven.repo.local"));
    } else {
      localRepoPath = new File(System.getProperty("user.home"), ".m2/repository");
    }

    LocalRepository localRepository = new LocalRepository(localRepoPath.getAbsolutePath());

    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    locator.setErrorHandler(
        new DefaultServiceLocator.ErrorHandler() {
          @Override
          public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
            LOGGER.error(
                "Service creation failed for {} with implementation {}", type, impl, exception);
          }
        });

    RepositorySystem repositorySystem = locator.getService(RepositorySystem.class);
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    session.setLocalRepositoryManager(
        repositorySystem.newLocalRepositoryManager(session, localRepository));
    session.setOffline(req.getOffline());

    DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
    DefaultRemoteRepositoryManager repositoryManager = new DefaultRemoteRepositoryManager();

    RemoteRepository remoteRepository =
        new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/")
            .build();
    List<RemoteRepository> remoteRepositories;

    if (req.getOffline()) {
      remoteRepositories = new ArrayList<>();
    } else {
      remoteRepositories = new ArrayList<>();
      remoteRepositories.add(remoteRepository);
    }

    DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest();
    modelBuildingRequest.setUserProperties(System.getProperties());
    modelBuildingRequest.setSystemProperties(System.getProperties());
    modelBuildingRequest.setPomFile(req.getPomFile());
    modelBuildingRequest.setProcessPlugins(false);
    modelBuildingRequest.setModelSource(new FileModelSource(req.getPomFile()));

    ProjectModelResolver modelResolver =
        new ProjectModelResolver(
            session, null, repositorySystem, repositoryManager, remoteRepositories, null, null);
    modelBuildingRequest.setModelResolver(modelResolver);

    ModelBuildingResult modelBuildingResult;
    try {
      modelBuildingResult = modelBuilder.build(modelBuildingRequest);
    } catch (ModelBuildingException e) {
      LOGGER.warn("Oops: ", e);
      throw e;
    }

    return new EmbedderFacadeResponse(
        modelBuildingResult, session, repositorySystem, remoteRepositories);
  }

  public static EmbedderFacade getInstance() {
    return instance;
  }

  public static class EmbedderFacadeRequest {
    private final boolean offline;
    private final File localRepositoryPath;
    private final File pomFile;
    private final Collection<String> activeProfileIds;
    private final Collection<String> inactiveProfileIds;

    public EmbedderFacadeRequest(
        boolean offline,
        File localRepositoryPath,
        File pomFile,
        Collection<String> activeProfileIds,
        Collection<String> inactiveProfileIds) {
      this.offline = offline;
      this.localRepositoryPath = localRepositoryPath;
      this.pomFile = pomFile;
      this.activeProfileIds = activeProfileIds != null ? activeProfileIds : Collections.emptyList();
      this.inactiveProfileIds =
          inactiveProfileIds != null ? inactiveProfileIds : Collections.emptyList();
    }

    public boolean getOffline() {
      return offline;
    }

    public boolean isOffline() {
      return offline;
    }

    public File getLocalRepositoryPath() {
      return localRepositoryPath;
    }

    public File getPomFile() {
      return pomFile;
    }

    public Collection<String> getActiveProfileIds() {
      return activeProfileIds;
    }

    public Collection<String> getInactiveProfileIds() {
      return inactiveProfileIds;
    }
  }

  public static class EmbedderFacadeResponse {
    private final ModelBuildingResult modelBuildingResult;
    private final RepositorySystemSession session;
    private final RepositorySystem repositorySystem;
    private final List<RemoteRepository> remoteRepositories;

    public EmbedderFacadeResponse(
        ModelBuildingResult modelBuildingResult,
        RepositorySystemSession session,
        RepositorySystem repositorySystem,
        List<RemoteRepository> remoteRepositories) {
      this.modelBuildingResult = modelBuildingResult;
      this.session = session;
      this.repositorySystem = repositorySystem;
      this.remoteRepositories =
          remoteRepositories != null ? remoteRepositories : Collections.emptyList();
    }

    public ModelBuildingResult getModelBuildingResult() {
      return modelBuildingResult;
    }

    public RepositorySystemSession getSession() {
      return session;
    }

    public RepositorySystem getRepositorySystem() {
      return repositorySystem;
    }

    public List<RemoteRepository> getRemoteRepositories() {
      return remoteRepositories;
    }
  }
}
