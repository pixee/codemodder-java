package io.codemodder.codemods.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ContainerConfiguration {

    private static final String GET_URL = "http://localhost:%s?day=5";

    public GenericContainer testContainer = new GenericContainer(
            new ImageFromDockerfile()
                   .withFileFromPath("target", new File("/Users/arturo/IdeaProjects/codemodder-java/core-codemods/src/test/resources/test-applications/demo/target").toPath())
                    .withFileFromPath("Dockerfile", new File("/Users/arturo/IdeaProjects/codemodder-java/core-codemods/src/test/resources/test-applications/demo/Dockerfile").toPath())
    )
            .withExposedPorts(8080);

    @Test
    void test() throws IOException {
        testContainer.start();
        sendGET(GET_URL.formatted(testContainer.getMappedPort(8080)));
        System.out.println("GET DONE");
    }

    private void sendGET( final String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        int responseCode = con.getResponseCode();
        System.out.println("GET Response Code :: " + responseCode);
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            System.out.println(response);
        } else {
            System.out.println("GET request did not work.");
        }

    }


}
