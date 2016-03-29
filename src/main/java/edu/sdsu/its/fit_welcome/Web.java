package edu.sdsu.its.fit_welcome;

import com.google.gson.Gson;
import edu.sdsu.its.fit_welcome.Models.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Primary Web Interactions
 *
 * @author Tom Paulus
 *         Created on 12/11/15.
 */
@Path("/")
public class Web {
    private static final Logger LOGGER = Logger.getLogger(Web.class);
    private static final Gson GSON = new Gson();

    @Path("login")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@QueryParam("id") final String uid) {
        LOGGER.info("Recieved Request: [GET] LOGIN - id = " + uid);

        int id = User.parseSwipe(uid);
        Staff staff = Staff.getStaff(id);
        User user = (staff == null) ? User.getUser(id) : null;

        if (user == null && staff == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(GSON.toJson(new ErrorMessage("User not Found"))).build();
        }

        Login login = new Login(staff != null ? staff : user, staff != null, Acutiy.getAppt(staff != null ? staff : user));
        return Response.status(Response.Status.OK).entity(GSON.toJson(login)).build();
    }


    /**
     * Get the status of a Staff Member's Clock
     *
     * @param id {@link int} Staff Member's ID
     * @return {@link Response} True = Clocked IN & False = Clocked OUT
     */
    @Path("clock/status")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getClockStatus(@QueryParam("id") final int id) {
        LOGGER.info("Recieved Request: [GET] CLOCK/STATUS - id = " + id);

        Staff staff = Staff.getStaff(id);
        if (staff == null || !staff.clockable) {
            Response.status(Response.Status.NOT_ACCEPTABLE).entity(GSON.toJson(new ErrorMessage("ID does not have a Clock."))).build();
        }

        final boolean status = new Clock(staff).getStatus();
        return Response.status(Response.Status.OK).entity(status).build();
    }


    /**
     * Confirmation Page
     *
     * @param id            {@link String} User's ID
     * @param goal          {@link String} User's Goal
     * @param hasAppt       {@link String} If an Acuity Appointment was found.
     * @param appointmentID {@link String} Appointment ID for the appointment that was found
     * @param source        {@link String} Which page the user came from
     * @param param         {@link String} Problem they are having (Meet with an ID)
     * @return {@link Response} Response
     */
    @Path("conf")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response confirmation(@QueryParam("id") final String id,
                                 @QueryParam("goal") final String goal,
                                 @QueryParam("has_appt") final String hasAppt,
                                 @QueryParam("apptID") final String appointmentID,
                                 @QueryParam("source") final String source,
                                 @QueryParam("param") final String param) {

        LOGGER.info(String.format("Recieved Request: [GET] CONF - id = %s & goal - %s & has_appt - %s & apptID - %s & source - %s & param - %s", id, goal, hasAppt, appointmentID, source, param));

        final Staff staff = Staff.getStaff(id);
        final User user = (staff == null) ? User.getUser(id) : null;

        final Quote quote = Quote.getRandom();

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("FIRST", staff != null ? staff.firstName : user.firstName);
        params.put("QUOTE", quote.text);
        params.put("QUOTEAUTHOR", quote.author);

        if ("staff".equals(source)) {
            boolean new_status = new Clock(staff).toggle();

            params.put("ACTION", new_status ? "Clocked In" : "Clocked Out");

            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.STAFF_CONFIRMATION, params)).build();
        } else if ("Schedule ParScore".equals(goal)) {
            try {
                final URI redirect = new URIBuilder()
                        .setPath("schedule")
                        .setParameter("first", staff != null ? staff.firstName : user.firstName)
                        .setParameter("last", staff != null ? staff.lastName : user.lastName)
                        .setParameter("email", staff != null ? staff.email.toLowerCase() : user.email.toLowerCase())
                        .build();

                return Response.seeOther(redirect).build();
            } catch (URISyntaxException e) {
                LOGGER.warn("Problem Creating Redirect URI", e);
            }
        } else if ("Meet an ID".equals(goal)) {
            if (param == null) {
                try {
                    final URI redirect = new URIBuilder()
                            .setPath("problemSelect")
                            .setParameter("id", Integer.toString(staff != null ? staff.id : user.id))
                            .build();

                    return Response.seeOther(redirect).build();
                } catch (URISyntaxException e) {
                    LOGGER.warn("Problem Creating Redirect URI", e);
                }
            } else {
                new Event(user, goal, param).logEvent();

                params.put("NOTE", "A FIT Consultant will be with you shortly!<br>");
                return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
            }
        } else if ("appt_found".equals(source)) {
            if ("no".equals(hasAppt.toLowerCase())) {
                try {
                    final URI redirect = new URIBuilder()
                            .setPath("welcome")
                            .setParameter("id", id)
                            .setParameter("skip_sch", "yes")
                            .setParameter("appt_id", appointmentID)
                            .build();

                    return Response.seeOther(redirect).build();
                } catch (URISyntaxException e) {
                    LOGGER.warn("Problem Creating Redirect URI", e);
                }
            } else {
                new Thread() {
                    @Override
                    public void run() {
                        LOGGER.info("Starting new Thread to update Acuity Appointment");
                        Acutiy.checkIn(Integer.parseInt(appointmentID));
                    }
                }.start();

                new Event(user, goal, "Appointment ID: " + appointmentID).logEvent();

                params.put("NOTE", "Let us know if there is anything we can<br>\n" +
                        "            do to make your visit more productive!");
                return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
            }
        } else if ("Use ParScore".equals(goal)) {
            if (appointmentID != null && appointmentID.length() > 0) {   // Catch Users who decline ParScore at first, but then select Par Score
                new Event(user, goal, "Appointment ID: " + appointmentID).logEvent();

                new Thread() {
                    @Override
                    public void run() {
                        LOGGER.info("Starting new Thread to update Acuity Appointment");
                        Acutiy.checkIn(Integer.parseInt(appointmentID));
                    }
                }.start();

                params.put("NOTE", "Let us know if there is anything we can<br>\n" +
                        "            do to make your visit more productive!");
                return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
            } else {
                new Event(user, goal, "Walk In").logEvent();
                params.put("NOTE", "ParScore Scanning is in High Demand!</ br> We recommend that you schedule an appointment ahead of time. " +
                        "<br><br>Please check with the FIT Center Consultant regarding machine availability.");

                return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
            }
        } else if ("Other".equals(goal)) {
            if (param == null) {
                try {
                    final URI redirect = new URIBuilder()
                            .setPath("otherSelect")
                            .setParameter("id", Integer.toString(staff != null ? staff.id : user.id))
                            .build();

                    return Response.seeOther(redirect).build();
                } catch (URISyntaxException e) {
                    LOGGER.warn("Problem Creating Redirect URI", e);
                }
            } else {
                new Event(staff != null ? staff : user, param, "").logEvent();

                params.put("NOTE", "Let us know if there is anything we can<br>\n" +
                        "            do to make your visit more productive!");
                return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
            }
        } else {
            new Event(staff != null ? staff : user, goal, "").logEvent();

            params.put("NOTE", "Let us know if there is anything we can<br>\n" +
                    "            do to make your visit more productive!");
            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Request").build();
    }

    /**
     * Schedule Acuity Appointment Page
     *
     * @param firstName {@link String} User's First Name
     * @param lastName  {@link String} User's Last Name
     * @param email     {@link String} User's Email
     * @return {@link Response} Response
     */
    @Path("schedule")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response schedule(@QueryParam("first") final String firstName, @QueryParam("last") final String lastName, @QueryParam("email") final String email) {
        LOGGER.info(String.format("Recieved Request: [GET] SCHEDULE - first = %s & last - %s & email - %s", firstName, lastName, email));


        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("FIRST", firstName);
        params.put("LAST", lastName);
        params.put("EMAIL", email);

        return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.SCHEDULE, params)).build();
    }

    /**
     * Problem Selection Page
     *
     * @param id {@link String} User's ID
     * @return {@link Response} Response
     */
    @Path("problemSelect")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response problemSelect(@QueryParam("id") final String id) {
        LOGGER.info(String.format("Recieved Request: [GET] PROBLEMSELECT - id = %s", id));

        final Staff staff = Staff.getStaff(id);
        final User user = (staff == null) ? User.getUser(id) : null;

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("REDID", Integer.toString(staff != null ? staff.id : user.id));
        params.put("FIRST", staff != null ? staff.firstName : user.firstName);

        return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.MEET_ID, params)).build();
    }

    /**
     * Other Selection Page
     *
     * @param id {@link String} User's ID
     * @return {@link Response} Response
     */
    @Path("otherSelect")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response otherSelect(@QueryParam("id") final String id) {
        LOGGER.info(String.format("Recieved Request: [GET] OTHERSELECT - id = %s", id));

        final Staff staff = Staff.getStaff(id);
        final User user = (staff == null) ? User.getUser(id) : null;

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("REDID", Integer.toString(staff != null ? staff.id : user.id));
        params.put("FIRST", staff != null ? staff.firstName : user.firstName);

        return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.OTHER, params)).build();
    }

    public static class ErrorMessage {
        String message;

        public ErrorMessage(String message) {
            this.message = message;
        }
    }

}