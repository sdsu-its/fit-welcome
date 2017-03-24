package edu.sdsu.its.Blackboard;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.sdsu.its.Blackboard.Models.DataSource;
import edu.sdsu.its.Blackboard.Models.User;
import edu.sdsu.its.Vault;
import org.apache.log4j.Logger;

import java.util.*;


/**
 * TODO JavaDoc
 *
 * @author Tom Paulus
 *         Created on 3/24/17.
 */
public class Users {
    private static final Logger LOGGER = Logger.getLogger(Users.class);
    private static final Map<String, DataSource> DSK_MAP = new HashMap<>();

    /**
     * Retrieve all users from the Blackboard API
     * <p>
     * At least one of the entitlements 'system.user.VIEW' or 'user.VIEW' are needed.
     * Note: Users with the 'SystemAdmin' role are only included in the results if the logged on user also has this role.
     *
     * @param limit Stop paging after N Users to Pull (0 pulls all users)
     * @return {@link User[]} All Users
     */
    public static User[] getAllUsers(int limit) {
        String baseURL = Vault.getParam("bb-url");
        String endpoint = "/learn/api/public/v1/users";
        List<User> users = new ArrayList<>();

        try {
            while (endpoint != null && (limit == 0 || users.size() <= limit)) {
                final HttpResponse httpResponse = Unirest.get(baseURL + endpoint)
                        .header("Authorization", "Bearer " + Auth.getToken())
                        .asString();

                LOGGER.debug("Request to get Users returned status - " + httpResponse.getStatus());
                if (httpResponse.getStatus() / 100 != 2) return null;

                Gson gson = new Gson();
                ResponsePayload payload = gson.fromJson(httpResponse.getBody().toString(), ResponsePayload.class);
                users.addAll(Arrays.asList(payload.results));

                endpoint = payload.paging.get("nextPage");
            }
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        for (User user : users)
            setDSK(user);

        return users.toArray(new User[]{});
    }

    private static void setDSK(User user) {
        final String dataSourceId = user.dataSourceId;
        if (!DSK_MAP.containsKey(dataSourceId)) {
            DSK_MAP.put(dataSourceId, DSK.getDataSoruce(dataSourceId));
        }

        user.DSK = DSK_MAP.get(dataSourceId);
    }

    private static class ResponsePayload {
        User[] results;
        Map<String, String> paging;
    }
}
