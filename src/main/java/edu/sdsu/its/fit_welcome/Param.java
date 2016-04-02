package edu.sdsu.its.fit_welcome;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Communicate with the Centralized Parameter Server
 *
 * @author Tom Paulus
 *         Created on 12/10/15.
 */
public class Param {
    final private static String URL = System.getenv("KSPATH");
    final private static String KEY = System.getenv("KSKEY");
    final private static String APP_NAME = System.getenv("WELCOME_APP");
    private static final Logger LOGGER = Logger.getLogger(Param.class);

    public static boolean testConnection() {
        try {
            final URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost(URL)
                    .setPath("/rest/client/echo")
                    .addParameter("m", "test")
                    .build();

            final ClientResponse response = get(uri);
            LOGGER.debug(String.format("Connection Test Returned with status: %d", response.getStatus()));
            return response.getStatus() == 200;
        } catch (URISyntaxException e) {
            LOGGER.error("Problem forming Connection URI - ", e);
            return false;
        }
    }

    /**
     * Get Parameter form the Default Application (whose name is an environment variable)
     *
     * @param parameterName {@link String} Parameter Name
     * @return {@link String} Parameter Value
     */
    public static String getParam(final String parameterName) {
        return getParam(APP_NAME, parameterName);
    }

    /**
     * Retrieve Param from Key Server
     *
     * @param applicationName {@link String} Application that the parameter is associated with
     * @param parameterName   {@link String} Parameter Name
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

            final ClientResponse response = get(uri);
            return response.getEntity(String.class);
        } catch (URISyntaxException e) {
            LOGGER.error("problem forming Connection URI - ", e);
            return "";
        }
    }

    /**
     * Make HTTP Get requests and return the Response form the Server.
     *
     * @param uri {@link URI} URI used to make get Request.
     * @return {@link ClientResponse} Response from get Request.
     */
    private static ClientResponse get(final URI uri) {
        LOGGER.info("Making a get request to: " + uri.toString());

        final Client client = Client.create();
        final WebResource webResource = client.resource(uri);

        ClientResponse response;
        try {
            response = webResource.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
            if (response.getStatus() != 200) {
                LOGGER.error("Error Connecting to Key Server - HTTP Error Code: " + response.getStatus());
            }
        } catch (UniformInterfaceException e) {
            response = null;
            LOGGER.error("Error connecting to Key Server Server", e);
        }

        return response;
    }

    public static void main(String[] args) {
        System.out.println(Param.getParam("Acuity", "ParScore Calendar"));
    }
}

