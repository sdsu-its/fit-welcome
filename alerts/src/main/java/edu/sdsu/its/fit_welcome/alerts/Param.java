package edu.sdsu.its.fit_welcome.alerts;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Communicate with Centralized Parameter Server
 *
 * @author Tom Paulus
 *         Created on 12/10/15.
 */
public class Param {
    final private static String URL = System.getenv("KSPATH");
    final private static String KEY = System.getenv("KSKEY");
    private static Logger Log = Logger.getLogger(Param.class);


    /**
     * Retrieve Param from Key Server
     *
     * @param applicationName {@link String} Application that the parameter is associated with
     * @param parameterName   {@link String } Parameter Name
     * @return {@link String} Parameter Value
     */
    public static String getParam(final String applicationName, final String parameterName) {
        try {
            final URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost(URL)
                    .setPath("/rest/client/param")
                    .addParameter("key", KEY)
                    .addParameter("app", applicationName)
                    .addParameter("name", parameterName)
                    .build();

            return get(uri);
        } catch (URISyntaxException e) {
            Logger.getLogger(Param.class).error("problem forming Connection URI - ", e);
            return "";
        }
    }

    /**
     * Make HTTP Get requests and return the Response form the Server.
     *
     * @param uri {@link URI} URI used to make get Request.
     * @return {@link String} Response Content from get Request.
     */
    private static String get(final URI uri) {
        String s = null;
        try {
            Log.debug(String.format("Making a GET Request to: \"%s\"", uri));

            HttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet(uri);

            HttpResponse response = client.execute(get);

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            Log.debug("Get request to Key Server Returned: " + result);
            s = result.toString();
        } catch (IOException e) {
            Log.warn("Problem communicating with KeyServer", e);
        }

        return s;
    }

    public static void main(String[] args) {
        System.out.println(Param.getParam("Acuity", "ParScore Calendar"));
    }
}

