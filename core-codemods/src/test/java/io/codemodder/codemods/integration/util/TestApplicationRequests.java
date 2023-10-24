package io.codemodder.codemods.integration.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

public class TestApplicationRequests {
  private TestApplicationRequests() {}

  private static final String ERROR_RESPONSE_MESSAGE = "Request failed with status: %s";

  public static String doRequest(final String testUrl, final String httpVerb) throws IOException {

    return switch (httpVerb) {
      case "GET" -> sendRequest(testUrl, "GET");

      case "POST" -> sendRequest(testUrl, "POST");

      case "PUT" -> sendRequest(testUrl, "PUT");

      case "DELETE" -> sendRequest(testUrl, "DELETE");

      default -> throw new IllegalStateException("Unexpected value: " + httpVerb);
    };
  }

  private static String sendRequest(final String url, final String httpVerb) throws IOException {

    URL objUrl = new URL(url);
    HttpURLConnection con = (HttpURLConnection) objUrl.openConnection();
    con.setRequestMethod(httpVerb);
    int responseCode = con.getResponseCode();

    if (responseCode == HttpURLConnection.HTTP_OK) { // success
      InputStream input = con.getInputStream();
      return IOUtils.toString(input, "UTF-8");
    } else {
      con.disconnect();
      throw new IOException(ERROR_RESPONSE_MESSAGE.formatted(responseCode));
    }
  }
}
