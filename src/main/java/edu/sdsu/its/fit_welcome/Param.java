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
 * TODO JavaDoc
 *
 * @author Tom Paulus
 *         Created on 12/10/15.
 */
public class Param {
    final private static String URL = System.getenv("KSPATH");
    final private static String KEY = System.getenv("KSKEY");


    /**
     * Retrieve Param from Key Server
     *
     * @param applicationName {@link String} Application that the parameter is associated with
     * @param parameterName {@link String } Parameter Name
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
            Logger.getLogger(Param.class).error("problem forming Connection URI - ", e);
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
        Logger.getLogger(Param.class).info("Making a get request to: " + uri.toString());

        final Client client = Client.create();
        final WebResource webResource = client.resource(uri);

        ClientResponse response;
        try {
            response = webResource.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
            if (response.getStatus() != 200) {
                Logger.getLogger(Param.class).error("Error Connecting to Key Server - HTTP Error Code: " + response.getStatus());
            }
        } catch (UniformInterfaceException e) {
            response = null;
            Logger.getLogger(Param.class).error("Error connecting to Key Server Server", e);
        }

        return response;
    }

    public static void main(String[] args) {
        System.out.println(Param.getParam("Acuity", "ParScore Calendar"));
    }
}

