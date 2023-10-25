package ai.pixee.integration.replaceapachedefaulthttpclient.api;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class TestController {

    private static final String COUNTRY_CAPITAL_URL = "https://restcountries.com/v3.1/name/%s?fields=capital";
    @GetMapping("/test/country/{countryName}/capital")
    public String getCapitalByCountryName(@PathVariable final String countryName) throws IOException {

        CloseableHttpClient httpClient = new DefaultHttpClient();

        HttpGet httpGet = new HttpGet(COUNTRY_CAPITAL_URL.formatted(countryName));

        try {
            // Execute the request
            HttpResponse response = httpClient.execute(httpGet);

            // Check the response status code
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                // If the response status code is 200 (OK), read and print the response content
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONArray jsonArray = new JSONArray(jsonResponse);

                if (jsonArray.length() > 0) {
                    JSONArray capitalArray = jsonArray.getJSONObject(0).optJSONArray("capital");
                    if (capitalArray != null && capitalArray.length() > 0) {
                        return capitalArray.getString(0);
                    }
                }

                return "Capital data not found for the given country.";

            } else {
                return  "Request failed with status code: " + statusCode;
            }
        } catch (Exception e) {
            return "Error: Exception occurred while making the request " + e.getMessage();
        } finally {
            httpClient.close();
        }
    }
}
