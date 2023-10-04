package io.codemodder.codemods.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;

public class DockerContainerFactory {

    public static GenericContainer<?> createContainer(final String codemodId, final String runCodemod) {
        return new GenericContainer(
                new ImageFromDockerfile()
                        .withFileFromPath("core-codemods", new File("/Users/arturo/IdeaProjects/codemodder-java/core-codemods").toPath())
                        .withFileFromPath("Dockerfile", new File("/Users/arturo/IdeaProjects/codemodder-java/core-codemods/src/test/java/io/codemodder/codemods/integration/Dockerfile").toPath())
                        .withBuildArg("RUN_CODEMOD", runCodemod)
                        .withBuildArg("CODEMOD_ID", codemodId)
        )
                .withExposedPorts(8080);
    }
}
