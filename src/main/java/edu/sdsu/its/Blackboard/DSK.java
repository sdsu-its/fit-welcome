package edu.sdsu.its.Blackboard;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.sdsu.its.Blackboard.Models.DataSource;
import edu.sdsu.its.Vault;
import org.apache.log4j.Logger;

/**
 * Manage DSK (Data Source Key) Blackboard s
 *
 * @author Tom Paulus
 *         Created on 2/10/17.
 */
public class DSK {
    private static final Logger LOGGER = Logger.getLogger(DSK.class);

    /**
     * The 'system.datasource.manager.VIEW' entitlement is needed.
     *
     * @param id {@link String} Internal DSK ID
     * @return {@link DataSource[]} Data Sources Array
     */
    public static DataSource getDataSoruce(String id) {
        try {
            final HttpResponse httpResponse = Unirest.get(Vault.getParam("bb-url") + "/learn/api/public/v1/dataSources/" + id)
                    .header("Authorization", "Bearer " + Auth.getToken())
                    .asString();
            LOGGER.debug("Request to get DSK returned status - " + httpResponse.getStatus());
            if (httpResponse.getStatus() / 100 != 2) return null;

            Gson gson = new Gson();
            return gson.fromJson(httpResponse.getBody().toString(), DataSource.class);

        } catch (UnirestException e) {
            LOGGER.warn("Problem retrieving DSK from API", e);
        }

        return null;
    }

}