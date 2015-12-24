package edu.sdsu.its.fit_welcome;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import edu.sdsu.its.fit_welcome.Models.Appointment;
import edu.sdsu.its.fit_welcome.Models.User;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Communicate with the Acuity Scheduling API
 *
 * @author Tom Paulus
 *         Created on 12/12/15.
 */
public class Acutiy {
    private static final String USERID = Param.getParam("Acuity", "User ID");
    private static final String KEY = Param.getParam("Acuity", "API Key");

    private static String getCurrentTimeStamp(final String pattern) {
        SimpleDateFormat sdfDate = new SimpleDateFormat(pattern);
        Date now = new Date();
        return sdfDate.format(now);
    }

    /**
     * Get all appointments for the provided User for the current date
     * Matches based on First/Last Name only
     *
     * @param user {@link User} User for whom appointments should be pulled
     * @return {@link Appointment} User's Appointment, null if none exists.
     */
    public static Appointment getAppt(final User user) {
        Appointment[] appointments = getToday();
        for (Appointment appointment : appointments) {
            if (appointment.firstName.equals(user.firstName) &&
                    appointment.lastName.equals(user.lastName)) {
                return appointment;
            }
        }

        return null;
    }

    /**
     * Get all appointments for Today
     *
     * @return {@link Appointment[]} All appointments for Today
     */
    public static Appointment[] getToday() {
        Appointment[] result;
        try {
            final URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("acuityscheduling.com/api/v1/")
                    .setPath("appointments")
                    .setParameter("minDate", getCurrentTimeStamp("yyyy-MM-dd"))
                    .setParameter("maxDate", getCurrentTimeStamp("yyyy-MM-dd"))
                    .setParameter("calendarID", Param.getParam("Acuity", "ParScore Calendar"))
                    .setParameter("canceled", "false")
                    .build();

            final ClientResponse response = get(uri);

            final Gson gson = new Gson();
            result = gson.fromJson(response.getEntity(String.class), Appointment[].class);


        } catch (URISyntaxException e) {
            result = null;
            Logger.getLogger(Acutiy.class).error("Could not formulate Acuity Schedule Request", e);
        }

        return result;
    }

    /**
     * Make HTTP Get requests and return the Response form the Server.
     *
     * @param uri {@link URI} URI used to make get Request.
     * @return {@link ClientResponse} Response from get Request.
     */
    private static ClientResponse get(final URI uri) {
        Logger.getLogger(Acutiy.class).info("Making a get request to: " + uri.toString());

        final Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(USERID, KEY));
        final WebResource webResource = client.resource(uri);

        ClientResponse response;

        try {
            response = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            if (response.getStatus() != 200) {
                Logger.getLogger(Acutiy.class).error("Error Connecting to Acuity - HTTP Error Code" + response.getStatus());
            }
        } catch (UniformInterfaceException e) {
            response = null;
            Logger.getLogger(Acutiy.class).error("Error connecting to Acuity Server", e);
        }

        return response;
    }
}
