package io.codemodder.codemods.integration.util;

import java.nio.file.Path;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Class to instantiate generic containers for codemod integration tests. The container generation
 * always will use the same dockerfile and context build.
 */
public class DockerContainerFactory {

  public static final String CODEMOD_ID_ARG_NAME = "CODEMOD_ID";
  public static final String RUN_CODEMOD_ARG_NAME = "RUN_CODEMOD";

  /**
   * @param codemodId ID of the codemod that could be executed inside the container.
   * @param runCodemod Boolean flag to indicate if the codemod will be executed.
   */
  public static GenericContainer<?> createContainer(
      final String codemodId, final boolean runCodemod) {
    return new GenericContainer(
            new ImageFromDockerfile()
                .withFileFromPath(
                    "test-applications", Path.of("src/test/resources/test-applications"))
                .withFileFromPath(
                    "Dockerfile",
                    Path.of("src/test/java/io/codemodder/codemods/integration/Dockerfile"))
                .withBuildArg(CODEMOD_ID_ARG_NAME, codemodId)
                .withBuildArg(RUN_CODEMOD_ARG_NAME, String.valueOf(runCodemod)))
        .withExposedPorts(8080);
  }
}
