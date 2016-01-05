package edu.sdsu.its.fit_welcome;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import edu.sdsu.its.fit_welcome.Models.User;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Communicate with the Acuity Scheduling API
 *
 * @author Tom Paulus
 *         Created on 12/12/15.
 */
public class Acutiy {
    public static final String CALID = Param.getParam("Acuity", "ParScore Calendar");
    private static final String USERID = Param.getParam("Acuity", "User ID");
    private static final String KEY = Param.getParam("Acuity", "API Key");
    private static final Logger Log = Logger.getLogger(Acutiy.class);

    private static String getCurrentTimeStamp(final String pattern) {
        // See https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html for Format of Pattern
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
        final Appointment[] appointments = getToday();
        final List<Appointment> appointmentsForUser = new ArrayList<Appointment>();

        for (Appointment appointment : appointments) {
            if (appointment.firstName.equals(user.firstName) &&
                    appointment.lastName.equals(user.lastName) &&
                    !appointment.notes.toLowerCase().contains("Checked in".toLowerCase())) {
                appointmentsForUser.add(appointment);
            }
        }


        //Find the first Appointment of the day for that user
        LocalTime earliestTime = new LocalTime("23:59:59"); // Last possible time fo the day
        Appointment earliestAppt = null;

        final DateTimeFormatter accuityFmt = DateTimeFormat.forPattern("hh:mma");

        for (Appointment a : appointmentsForUser) {
            LocalTime apptTime = accuityFmt.parseLocalTime(a.time);
            if (earliestTime.isAfter(apptTime)) {
                earliestTime = apptTime;
                earliestAppt = a;
            }
        }

        return earliestAppt;
    }

    /**
     * Get the appointment by Acuity's Appointment ID
     *
     * @param id {@link Integer} Appointment ID
     * @return {@link Appointment} The Appointment that coresponds to that ID
     */
    public static Appointment getAppt(final Integer id) {
        Appointment result = null;
        try {
            final URI uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("acuityscheduling.com/api/v1/appointments/")
                    .setPath(id.toString())
                    .build();

            final ClientResponse response = get(uri);

            final Gson gson = new Gson();
            result = gson.fromJson(response.getEntity(String.class), Appointment.class);
        } catch (URISyntaxException e) {
            Log.error("Could not formulate URI to fetch Acuity Appointment", e);
        }

        return result;
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
                    .setParameter("calendarID", CALID)
                    .setParameter("canceled", "false")
                    .build();

            final ClientResponse response = get(uri);

            final Gson gson = new Gson();
            result = gson.fromJson(response.getEntity(String.class), Appointment[].class);


        } catch (URISyntaxException e) {
            result = null;
            Log.error("Could not formulate Acuity Schedule Request", e);
        }

        return result;
    }

    /**
     * Check-in User for Specified Appointment
     *
     * @param appointmentID {@link Integer} Acuity AppointmentID
     */
    public static void checkIn(final Integer appointmentID) {
        final Appointment appointment = getAppt(appointmentID);
        final Appointment newAppointment = new Appointment();

        final String checkInText = "Checked in at " + getCurrentTimeStamp("hh:mma");
        newAppointment.notes = checkInText + ((appointment.notes.length() > 0) ? "\n" + appointment.notes : ""); // Checked In at TIME (\n Original Note) - if exists

        final URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("acuityscheduling.com/api/v1/appointments/")
                    .setPath(appointmentID.toString())
                    .build();

            final Gson gson = new Gson();
            put(uri, gson.toJson(newAppointment));

        } catch (URISyntaxException e) {
            Log.error("Could not formulate Acuity Schedule Request", e);
        }
    }

    /**
     * Make HTTP Get requests and return the Response form the Server.
     *
     * @param uri {@link URI} URI used to make get Request.
     * @return {@link ClientResponse} Response from get Request.
     */
    private static ClientResponse get(final URI uri) {
        Log.info("Making a get request to: " + uri.toString());

        final Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(USERID, KEY));
        final WebResource webResource = client.resource(uri);

        ClientResponse response;

        try {
            response = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
            if (response.getStatus() != 200) {
                Log.error("Error Connecting to Acuity - HTTP Error Code" + response.getStatus());
            }
        } catch (UniformInterfaceException e) {
            response = null;
            Log.error("Error connecting to Acuity Server", e);
        }

        return response;
    }

    private static ClientResponse put(final URI uri, final String payload) {
        Log.info("Making put request to: " + uri.toString());

        final Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(USERID, KEY));
        final WebResource webResource = client.resource(uri);

        ClientResponse response;

        response = webResource.accept(MediaType.WILDCARD_TYPE).entity(payload).put(ClientResponse.class);
        if (response.getStatus() != 200) {
            Log.error("Error Connecting to Acuity - HTTP Error Code" + response.getStatus());
        }

        return response;
    }

    /**
     * Models an appointment in Acuity
     */
    public static class Appointment {
        public Integer id;
        public String firstName;
        public String lastName;
        public String email;
        public String date;
        public String time;
        public String notes;

    }
}
