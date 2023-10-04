package io.codemodder.codemods.integration;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import java.io.IOException;

import static io.codemodder.codemods.integration.util.HttpClient.sendGET;

public class MoveSwitchDefaultCaseLastIntegrationTest {

    private static final String GET_URL = "http://localhost:%s?day=1";

    private final GenericContainer<?> originalCodeContainer = DockerContainerFactory.createContainer("move-switch-default-last","false");
    private final GenericContainer<?> transformedCodeContainer = DockerContainerFactory.createContainer("move-switch-default-last","true");

    @Test
    void run_codemod_should_not_change_behavior_in_transformed_code() throws IOException {

   originalCodeContainer.start();
   transformedCodeContainer.start();

        final String originalCodeResponse = sendGET(GET_URL.formatted(originalCodeContainer.getMappedPort(8080)));
        final String transformedCodeResponse = sendGET(GET_URL.formatted(transformedCodeContainer.getMappedPort(8080)));

        assertThat(transformedCodeResponse, equalTo(originalCodeResponse));
    }
}
