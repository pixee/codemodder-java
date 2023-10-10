package io.codemodder.codemods.integration.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestApplicationRequestUtil {

  public static String doRequest(final String testUrl, final String httpVerb) throws IOException {

    return switch (httpVerb) {
      case "GET" -> sendGET(testUrl);

      default -> throw new IllegalStateException("Unexpected value: " + httpVerb);
    };
  }

  private static String sendGET(final String url) throws IOException {
    URL obj = new URL(url);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    con.setRequestMethod("GET");
    int responseCode = con.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) { // success
      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();

      // print result
      return response.toString();
    } else {
      return "GET request did not work.";
    }
  }
}
