package io.codemodder.codemods.integration;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

import static io.codemodder.codemods.integration.util.HttpClient.sendGET;

public class MoveSwitchDefaultCaseLastIntegrationTest extends CodemodIntegrationTestSetup {

    private static final String CODEMOD_ID = "move-switch-default-last";
    private static final String TEST_ENDPOINT = "http://localhost:%s?day=1";

    @BeforeEach
    public void setup() {
        setupContainers(CODEMOD_ID);
    }
    @AfterEach
    public void tearDown() {
        stopContainers();
    }

    @Test
    void run_codemod_should_not_change_behavior_in_transformed_code() throws IOException {
   final String originalCodeResponse = testApplicationBehavior(originalCodeContainer);
   final String transformedCodeResponse = testApplicationBehavior(transformedCodeContainer);

   assertThat(transformedCodeResponse, equalTo(originalCodeResponse));
    }

    private String testApplicationBehavior(final GenericContainer<?> container) throws IOException {
       return sendGET(TEST_ENDPOINT.formatted(container.getMappedPort(8080)));
    }
}
