package edu.sdsu.its.API;

import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.sdsu.its.API.Models.SimpleMessage;
import edu.sdsu.its.Vault;
import edu.sdsu.its.Welcome.DB;
import edu.sdsu.its.API.Models.Staff;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Communicate with the Acuity Scheduling API
 *
 * @author Tom Paulus
 *         Created on 12/12/15.
 */
@Path("acuity")
public class Acutiy {
    private static final String USERID = Vault.getParam("Acuity", "User ID");
    private static final String KEY = Vault.getParam("Acuity", "API Key");
    private static final Logger LOGGER = Logger.getLogger(Acutiy.class);
    private static final DateTimeFormatter acuityFmt = DateTimeFormat.forPattern("hh:mma");

    private static final int PAST_DURATION = -1;  // Should be < 0
    private static final int UPCOMING_DURATION = 2; // Should be > 0
    private static final String TIMEZONE = "America/Los_Angeles";

    private static String getCurrentTimeStamp(final String pattern) {
        // See https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html for Format of Pattern
        SimpleDateFormat sdfDate = new SimpleDateFormat(pattern);
        Date now = new Date();
        return sdfDate.format(now);
    }

    /**
     * Get all upcoming appointments for the next N hours
     */
    public static Appointment[] getUpcoming() {
//        TODO Check if appointment type is allowed to be shown on the console
//        TODO Add field to Acuity Appointment Map for "Show" boolean

        final Appointment[] appointments = getToday();
        final List<Appointment> upcomingAppointments = new ArrayList<>();

        for (Appointment appointment : appointments) {
            if (appointment.notes.toLowerCase().contains("Checked in".toLowerCase())) {
                LOGGER.debug(String.format("Disregarding Appointment (ID: %d) - Already Checked In", appointment.id));
                // Already CheckedIn
                continue;
            }

            LocalTime apptTime = acuityFmt.parseLocalTime(appointment.time);

            LOGGER.debug(String.format("Checking if %s is an upcoming appointment", appointment.toString()));
            // Appointment times must meet the following criteria:
            // apptTime-UPCOMING_DURATION < NOW < apptTime+PAST_DURATION
            // The must be within the next UPCOMING_DURATION, or past PAST_DURATION Hours.
            final LocalTime now = LocalTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone(TIMEZONE)));
            Duration delta = new Duration(now.toDateTimeToday(), apptTime.toDateTimeToday());

            if (delta.getStandardHours() < PAST_DURATION) {
                LOGGER.debug("Not Valid - Happened to long ago in the past");
            } else if (delta.getStandardHours() > UPCOMING_DURATION) {
                LOGGER.debug("Not Valid - Is too far in the future");
            } else {
                LOGGER.debug("Valid - Falls within eligible range");
                upcomingAppointments.add(appointment);
            }

            LOGGER.debug(String.format("Found %d upcoming appointments", upcomingAppointments.size()));
        }

        upcomingAppointments.sort(Comparator.comparing(o -> acuityFmt.parseLocalTime(o.time)));
        return upcomingAppointments.toArray(new Appointment[]{});
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

            final HttpResponse response = get(uri);

            final Gson gson = new Gson();
            result = gson.fromJson(response.getBody().toString(), Appointment.class);
        } catch (URISyntaxException e) {
            LOGGER.error("Could not formulate URI to fetch Acuity Appointment", e);
        }

        return result;
    }

    /**
     * Get the next appointment for a User, given their ID
     *
     * @param id {@link Integer} User Id
     * @return {@link Appointment} Next Appointment, Null if non-existent
     */
    public static Appointment getApptByOwner(final Integer id) {
        for (Appointment appointment : getToday()) {
            if (appointment.getOwnerID() == id) {
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
                    .setParameter("canceled", "false")
                    .build();

            final HttpResponse response = get(uri);
            LOGGER.debug(String.format("Get Request to Acuity for Today's Events returned (%s) - %s", response.getStatus(), response.toString()));

            final Gson gson = new Gson();
            result = gson.fromJson(response.getBody().toString(), Appointment[].class);


        } catch (URISyntaxException e) {
            result = null;
            LOGGER.error("Could not formulate Acuity Schedule Request", e);
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
            HttpResponse response = put(uri, gson.toJson(newAppointment));
            result = gson.fromJson(response.getBody().toString(), Appointment.class);

        } catch (URISyntaxException e) {
            LOGGER.error("Could not formulate Acuity Schedule Request", e);
            result = null;
        }

        return result;
    }

    /**
     * Make HTTP Get request and return the Response form the Server.
     *
     * @param uri {@link URI} URI used to make get Request.
     * @return {@link HttpResponse} Response from get Request.
     */
    private static HttpResponse get(final URI uri) {
        LOGGER.info("Making a get request to: " + uri.toString());

        HttpResponse response;

        try {
            response = Unirest
                    .get(uri.toString())
                    .basicAuth(USERID, KEY)
                    .asJson();
        } catch (UnirestException e) {
            response = null;
            LOGGER.error("Error connecting to Acuity Server", e);
        }

        return response;
    }

    /**
     * Make an HTTP Put request and return the Response form the Server.
     *
     * @param uri     {@link URI} URI used to make get Request.
     * @param payload {@link String} PUT Payload
     * @return {@link HttpResponse} Response from Server
     */
    private static HttpResponse put(final URI uri, final String payload) {
        LOGGER.info("Making put request to: " + uri.toString());

        HttpResponse response;

        try {
            response = Unirest
                    .put(uri.toString())
                    .basicAuth(USERID, KEY)
                    .body(payload)
                    .asJson();
        } catch (UnirestException e) {
            response = null;
            LOGGER.error("Error connecting to Acuity Server", e);
        }

        if (response != null && response.getStatus() != 200) {
            LOGGER.error("Error Connecting to Acuity - HTTP Error Code" + response.getStatus());
            LOGGER.info("Request Returned - " + response.getBody().toString());
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
            HttpResponse response = get(uri);
            result = gson.fromJson(response.getBody().toString(), AppointmentType[].class);
        } catch (URISyntaxException e) {
            LOGGER.error("Could not formulate Acuity Schedule Request", e);
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
        LOGGER.info(String.format("Recieved Request: [POST] ACUITY/APPOINTMENTMAP - Requester: %s & Payload: %s", requester, payload));
        Gson gson = new Gson();

        Staff staff = (requester != null && requester.length() > 0) ? Staff.getStaff(Integer.parseInt(requester)) : null;
        if (staff == null || !staff.admin) {
            LOGGER.warn("Unauthorized Request to POST ACUITY/APPOINTMENTMAP - ID: " + requester);
            return Response.status(Response.Status.FORBIDDEN).entity(gson.toJson(new SimpleMessage("ID is not a valid Admin ID"))).build();
        }

        AppointmentType[] appointmentTypes = gson.fromJson(payload, AppointmentType[].class);
        for (AppointmentType appointmentType : appointmentTypes) {
            DB.setAppointmentTypeMatch(appointmentType);
        }
        return Response.status(Response.Status.ACCEPTED).entity(gson.toJson(new SimpleMessage("Success"))).build();
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

        public int getOwnerID() {
            for (Acutiy.Appointment.Form form : this.forms) {
                for (Acutiy.Appointment.Value value : form.values) {
                    if (value.name.contains("RedID") && value.value != null && value.value.length() > 0) {
                        int userID;
                        try {
                            userID = Integer.valueOf(value.value);
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid RedID Format");
                            continue;
                        }
                        return userID;
                    }
                }
            }

            return 0;
        }

        @SuppressWarnings("unused")
        class Form {
            public Value[] values;
        }

        @SuppressWarnings("unused")
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
