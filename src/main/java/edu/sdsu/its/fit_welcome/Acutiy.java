package edu.sdsu.its.fit_welcome;

import com.google.gson.Gson;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import edu.sdsu.its.fit_welcome.Models.Staff;
import edu.sdsu.its.fit_welcome.Models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
@Path("acuity")
public class Acutiy {
    private static final String USERID = Param.getParam("Acuity", "User ID");
    private static final String KEY = Param.getParam("Acuity", "API Key");
    private static final Logger Log = Logger.getLogger(Acutiy.class);
    private static final int FUZZY_THRESHOLD = 3;


    private static String getCurrentTimeStamp(final String pattern) {
        // See https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html for Format of Pattern
        SimpleDateFormat sdfDate = new SimpleDateFormat(pattern);
        Date now = new Date();
        return sdfDate.format(now);
    }

    private static boolean apptMatch(final Appointment appointment, final User user) {
        boolean match = false;
        // Check by ID
        for (Appointment.Form f : appointment.forms) {
            for (Appointment.Value v : f.values) {
                if (v.name.contains("RedID") && v.value != null && v.value.length() > 0) {
                    int userID;
                    try {
                        userID = Integer.valueOf(v.value);
                    } catch (NumberFormatException e) {
                        Log.warn("Invalid RedID Format");
                        continue;
                    }
                    if (userID == user.id) {
                        Log.debug(String.format("Direct Match by ID found for %s %s - AppointmentID: %d", user.firstName, user.lastName, appointment.id));
                        match = true;
                        break;
                    }
                }
            }
            if (match) break;
        }

        // Check by Name
        if (!match && appointment.firstName.equals(user.firstName) &&
                appointment.lastName.equals(user.lastName)) {
            Log.debug(String.format("Direct Match by Name found for %s %s - AppointmentID: %d", user.firstName, user.lastName, appointment.id));
            match = true;
        } else if (StringUtils.getLevenshteinDistance(appointment.firstName, user.firstName) <= FUZZY_THRESHOLD &&
                StringUtils.getLevenshteinDistance(appointment.lastName, user.lastName) <= FUZZY_THRESHOLD) {
            Log.debug(String.format("Fuzzy Match found for %s %s - AppointmentID: %d", user.firstName, user.lastName, appointment.id));
            match = true;
        }

        return match;
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
            if (appointment.notes.toLowerCase().contains("Checked in".toLowerCase())) {
                Log.debug(String.format("Disregarding Appointment (ID: %d) - Already Checked In", appointment.id));
                // Already CheckedIn
                continue;
            }

            if (apptMatch(appointment, user)) {
                appointmentsForUser.add(appointment);
            }
        }


        //Find the first Appointment of the day for that user
        LocalTime earliestTime = new LocalTime("23:59:59"); // Last possible time fo the day
        Appointment earliestAppt = null;

        final DateTimeFormatter acuityFmt = DateTimeFormat.forPattern("hh:mma");

        for (Appointment a : appointmentsForUser) {
            LocalTime apptTime = acuityFmt.parseLocalTime(a.time);
            if (earliestTime.isAfter(apptTime)) {
                earliestTime = apptTime;
                earliestAppt = a;
            }
        }

        Log.debug(String.format("Earliest appointment for User: %d is %s", user.id, earliestAppt != null ? earliestAppt.toString() : "No Appt Found."));
        return earliestAppt;
    }

    /**
     * Get the appointment by Acuity's Appointment ID
     *
     * @param id {@link Integer} Appointment ID
     * @return {@link Appointment} The Appointment that corresponds to that ID
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
                    .setParameter("canceled", "false")
                    .build();

            final ClientResponse response = get(uri);
            Log.debug(String.format("Get Request to Acuity for Today's Events returned (%s) - %s", response.getStatus(), response.toString()));

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
     * @return {@link Appointment} Updated Appointment
     */
    public static Appointment checkIn(final Integer appointmentID) {
        final Appointment appointment = getAppt(appointmentID);
        final Appointment newAppointment = new Appointment();
        Appointment result;

        final String checkInText = "Checked in at " + getCurrentTimeStamp("hh:mma");
        newAppointment.notes = checkInText + ((appointment.notes.length() > 0) ? "\n" + appointment.notes : ""); // Checked In at TIME (\n Original Note) - if exists

        final URI uri;
        try {
            uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("acuityscheduling.com/api/v1/appointments/")
                    .setPath(appointmentID.toString())
                    .setParameter("admin", "true") // Bypasses Required Field Validations.
                    .build();

            final Gson gson = new Gson();
            ClientResponse response = put(uri, gson.toJson(newAppointment));
            result = gson.fromJson(response.getEntity(String.class), Appointment.class);

        } catch (URISyntaxException e) {
            Log.error("Could not formulate Acuity Schedule Request", e);
            result = null;
        }

        return result;
    }

    /**
     * Make HTTP Get request and return the Response form the Server.
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
                Log.error("Error Connecting to Acuity - HTTP Error Code " + response.getStatus());
                Log.info("Request returned - " + response.getEntity(String.class));
            }
        } catch (UniformInterfaceException e) {
            response = null;
            Log.error("Error connecting to Acuity Server", e);
        }

        return response;
    }

    /**
     * Make an HTTP Put request and return the Response form the Server.
     *
     * @param uri     {@link URI} URI used to make get Request.
     * @param payload {@link String} PUT Payload
     * @return {@link ClientResponse} Response from Server
     */
    private static ClientResponse put(final URI uri, final String payload) {
        Log.info("Making put request to: " + uri.toString());

        final Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter(USERID, KEY));
        final WebResource webResource = client.resource(uri);

        ClientResponse response;

        response = webResource.accept(MediaType.WILDCARD_TYPE).entity(payload).put(ClientResponse.class);

        if (response.getStatus() != 200) {
            Log.error("Error Connecting to Acuity - HTTP Error Code" + response.getStatus());
            Log.info("Request Returned - " + response.getEntity(String.class));

        }

        return response;
    }

    /**
     * Get all of the appointments currently offered in Acuity
     *
     * @return {@link AppointmentType[]} All AppointmentTypes
     */
    public AppointmentType[] getAppointmentTypes() {
        final URI uri;
        AppointmentType[] result = null;
        try {
            uri = new URIBuilder()
                    .setScheme("https")
                    .setHost("acuityscheduling.com/api/v1/appointment-types")
                    .setParameter("deleted", "false")
                    .build();

            final Gson gson = new Gson();
            ClientResponse response = get(uri);
            result = gson.fromJson(response.getEntity(String.class), AppointmentType[].class);
        } catch (URISyntaxException e) {
            Log.error("Could not formulate Acuity Schedule Request", e);
        }
        if (result != null) {
            for (int at = 0; at < result.length; at++) {
                result[at] = DB.getAppointmentTypeMatch(result[at]);
            }
        }

        return result;
    }

    /**
     * Get the Appointment Mapping.
     * {@see getAppointmentTypes()}
     *
     * @return {@link Response} JSON Array of All Appointment Types with their Mapping
     */
    @Path("appointmentMap")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppointmentMap() {
        Gson gson = new Gson();
        return Response.status(Response.Status.OK).entity(gson.toJson(getAppointmentTypes())).build();
    }

    /**
     * Set the Appointment Mapping.
     * Requires Admin Level User.
     *
     * @param requester {@link String} Requester's ID
     * @param payload   {@link String} JSON Array of AppointmentTypes
     * @return {@link Response} Completion Status or Error Message
     */
    @Path("appointmentMap")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setAppointmentMap(@HeaderParam("REQUESTER") final String requester, final String payload) {
        Log.info(String.format("Recieved Request: [POST] ACUITY/APPOINTMENTMAP - Requester: %s & Payload: %s", requester, payload));
        Gson gson = new Gson();

        Staff staff = (requester != null && requester.length() > 0) ? Staff.getStaff(Integer.parseInt(requester)) : null;
        if (staff == null || !staff.admin) {
            Log.warn("Unauthorized Request to POST ACUITY/APPOINTMENTMAP - ID: " + requester);
            return Response.status(Response.Status.FORBIDDEN).entity(gson.toJson(new Web.SimpleMessage("ID is not a valid Admin ID"))).build();
        }

        AppointmentType[] appointmentTypes = gson.fromJson(payload, AppointmentType[].class);
        for (AppointmentType appointmentType : appointmentTypes) {
            DB.setAppointmentTypeMatch(appointmentType);
        }
        return Response.status(Response.Status.ACCEPTED).entity(gson.toJson(new Web.SimpleMessage("Success"))).build();
    }

    /**
     * Models an appointment in Acuity
     */
    public static class Appointment {
        public Integer id;
        public String type;
        public Integer appointmentTypeID;
        public String firstName;
        public String lastName;
        public String email;
        public String date;
        public String time;
        public String notes;
        public Form[] forms;

        @Override
        public String toString() {
            return String.format("%s at %s (ID: %d)", type, time, id);
        }

        class Form {
            public Value[] values;
        }

        class Value {
            public int id;
            public int fieldID;
            public String value;
            public String name;
        }
    }

    /**
     * Models an appointment type in Acuity
     */
    public static class AppointmentType {
        public Integer id;
        public String name;

        public String eventText;
        public String eventParams;

        public AppointmentType(Integer id) {
            this.id = id;
        }
    }
}
