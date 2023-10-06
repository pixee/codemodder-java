package io.codemodder.codemods.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;

public class DockerContainerFactory {

    public static final String CODEMOD_ID_ARG_NAME = "CODEMOD_ID";
    public static final String RUN_CODEMOD_ARG_NAME = "RUN_CODEMOD";

    public static GenericContainer<?> createContainer(final String codemodId, final boolean runCodemod) {
        return new GenericContainer(
                new ImageFromDockerfile()
                        // TODO: remove absolute path for the build context resources
                        .withFileFromPath("test-applications", new File("/Users/arturo/IdeaProjects/codemodder-java/core-codemods/src/test/resources/test-applications").toPath())
                        .withFileFromPath("Dockerfile", new File("/Users/arturo/IdeaProjects/codemodder-java/core-codemods/src/test/java/io/codemodder/codemods/integration/Dockerfile").toPath())
                        .withBuildArg(CODEMOD_ID_ARG_NAME, codemodId)
                        .withBuildArg(RUN_CODEMOD_ARG_NAME, String.valueOf(runCodemod))
        )
                .withExposedPorts(8080);
    }
}
