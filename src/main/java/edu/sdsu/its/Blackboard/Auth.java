package edu.sdsu.its.Blackboard;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.sdsu.its.Vault;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;

/**
 * Authenticate with the Blackboard.
 *
 * @author Tom Paulus
 *         Created on 1/27/17.
 */
public class Auth {
    private static final Logger LOGGER = Logger.getLogger(Auth.class);
    private static String token = null;

    private static final String BB_API_SECRET = Vault.getParam("bb-API-secret");

    private static void BbAuthenticate() throws UnirestException {
        HttpResponse<String> httpResponse = Unirest.post(
                Vault.getParam("bb-url") + "/learn/api/public/v1/oauth2/token")
                .queryString("grant_type", "client_credentials")
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
                .basicAuth(Vault.getParam(BB_API_SECRET, "key"),
                        Vault.getParam(BB_API_SECRET, "secret"))
                .asString();

        if (httpResponse.getStatus() / 100 != 2) {
            LOGGER.fatal("Problem Authenticating with Learn Server", new Exception(httpResponse.getBody()));
            return;
        }

        Gson gson = new Gson();
        AuthPayload payload = gson.fromJson(httpResponse.getBody(), AuthPayload.class);
        LOGGER.debug("Received token from LEARN Server - " + payload.access_token);
        token = payload.access_token;
    }

    public static String getToken() {
        try {
            if (token == null) BbAuthenticate();
        } catch (UnirestException e) {
            LOGGER.error("Problem Authenticating with Learn Server", e);
        }
        return token;
    }

    public static String resetToken() {
        token = null;
        return getToken();
    }

    /**
     * Models Blackboard Authentication Payload
     *
     * @author Tom Paulus
     *         Created on 1/27/17.
     */
    private static class AuthPayload {
        public String access_token;
        public String token_type;
        public int expires_in;
    }

}
