package io.codemodder.codemods.integration;

import org.testcontainers.containers.GenericContainer;

public class CodemodIntegrationTestSetup {

    public GenericContainer<?> originalCodeContainer;
    public GenericContainer<?> transformedCodeContainer;

    public void setupContainers(final String codemodId) {
        originalCodeContainer = DockerContainerFactory.createContainer(codemodId,false);
        transformedCodeContainer = DockerContainerFactory.createContainer(codemodId,true);

        originalCodeContainer.start();
        transformedCodeContainer.start();
    }

    public void stopContainers() {
        originalCodeContainer.stop();
        transformedCodeContainer.stop();
    }
}
