package edu.sdsu.its.fit_welcome;

import edu.sdsu.its.fit_welcome.Models.Event;
import edu.sdsu.its.fit_welcome.Models.Quote;
import edu.sdsu.its.fit_welcome.Models.Staff;
import edu.sdsu.its.fit_welcome.Models.User;
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
    private final Logger Log = Logger.getLogger(getClass());

    /**
     * Welcome Page
     *
     * @param uid        {@link String} UserID (Either typed in or Magstripe Encoded)
     * @param skipAcuity {@link String} if "yes" Acuity will not be checked for appointments
     * @return {@link Response} Response
     */
    @Path("welcome")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response welcome(@QueryParam("id") final String uid, @QueryParam("skip_sch") final String skipAcuity) {
        Log.info(String.format("Recieved Request: [GET] WELCOME - id = %s & skip_sch - %s", uid, skipAcuity));

        int redid = User.parseSwipe(uid);

        Staff staff = Staff.getStaff(redid);
        User user = (staff == null) ? User.getUser(redid) : null;

        if (user == null && staff == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Pages.makePage(Pages.NOT_FOUND, new HashMap<String, String>())).build();
        }

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("FIRST", staff != null ? staff.firstName : user.firstName);
        params.put("REDID", Integer.toString(redid));

        if (staff != null && staff.clockable) {
            final boolean status = new Clock(staff).getStatus();
            params.put("STATUS", status ? "Clocked In" : "Clocked Out");
            params.put("VERB", status ? "Out" : "In");
            params.put("ADMIN", staff.admin ? "" : "style=\"display: none;\"");

            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.STAFF_WELCOME, params)).build();
        } else if (staff != null && staff.admin) {
            try {
                final URI redirect = new URIBuilder()
                        .setPath("admin")
                        .setParameter("id", Integer.toString(redid))
                        .build();

                return Response.seeOther(redirect).build();
            } catch (URISyntaxException e) {
                Log.warn("Problem Creating Redirect URI", e);
            }
        }

        Acutiy.Appointment appointment = !"yes".equals(skipAcuity) ? Acutiy.getAppt(user) : null;
        if (appointment != null) {
            params.put("TIME", appointment.time);
            params.put("APPTID", appointment.id.toString());
            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.APPT_FOUND, params)).build();
        }

        return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.WELCOME, params)).build();
    }

    /**
     * Confirmation Page
     *
     * @param id            {@link String} User's ID
     * @param goal          {@link String} User's Goal
     * @param hasAppt       {@link String} If an Acuity Appointment was found.
     * @param appointmentID {@link String} Appointment ID for the appointment that was found
     * @param source        {@link String} Which page the user came from
     * @param problem       {@link String} Problem they are having (Meet with an ID)
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
                                 @QueryParam("problem") final String problem) {

        Log.info(String.format("Recieved Request: [GET] CONF - id = %s & goal - %s & has_appt - %s & apptID - %s & source - %s & problem - %s", id, goal, hasAppt, appointmentID, source, problem));

        final User user = User.getUser(Integer.parseInt(id));

        final Quote quote = Quote.getRandom();

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("FIRST", user.firstName);
        params.put("QUOTE", quote.text);
        params.put("QUOTEAUTHOR", quote.author);

        if ("staff".equals(source)) {
            boolean new_status = new Clock(user).toggle();

            params.put("ACTION", new_status ? "Clocked In" : "Clocked Out");

            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.STAFF_CONFIRMATION, params)).build();
        } else if ("Schedule ParScore".equals(goal)) {
            try {
                final URI redirect = new URIBuilder()
                        .setPath("schedule")
                        .setParameter("first", user.firstName)
                        .setParameter("last", user.lastName)
                        .setParameter("email", user.email.toLowerCase())
                        .build();

                return Response.seeOther(redirect).build();
            } catch (URISyntaxException e) {
                Log.warn("Problem Creating Redirect URI", e);
            }
        } else if ("Meet an ID".equals(goal)) {
            if (problem == null) {
                try {
                    final URI redirect = new URIBuilder()
                            .setPath("problemSelect")
                            .setParameter("id", Integer.toString(user.id))
                            .build();

                    return Response.seeOther(redirect).build();
                } catch (URISyntaxException e) {
                    Log.warn("Problem Creating Redirect URI", e);
                }
            } else {
                new Event(user, goal, problem).logEvent();

                params.put("NOTE", "A FIT Consultant will be with you shortly!");
                return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
            }
        } else if ("appt_found".equals(source)) {
            if ("no".equals(hasAppt.toLowerCase())) {
                try {
                    final URI redirect = new URIBuilder()
                            .setPath("welcome")
                            .setParameter("id", id)
                            .setParameter("skip_sch", "yes")
                            .build();

                    return Response.seeOther(redirect).build();
                } catch (URISyntaxException e) {
                    Log.warn("Problem Creating Redirect URI", e);
                }
            } else {
                new Thread() {
                    @Override
                    public void run() {
                        Log.info("Starting new Thread to update Acuity Appointment");
                        Acutiy.checkIn(Integer.parseInt(appointmentID));
                    }
                }.start();

                new Event(user, goal, "Appointment ID: " + appointmentID).logEvent();

                params.put("NOTE", "Let us know if there is anything we can<br>\n" +
                        "            do to make your visit more productive!");
                return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
            }
        } else if ("Use ParScore".equals(goal)) {
            new Event(user, goal, "Walk In").logEvent();
            params.put("NOTE", "ParScore Scanning is in High Demand!</ br> We recommend that you schedule an appointment ahead of time. " +
                    "Please check with the FIT Center Consultant regarding machine availability.");

            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
        } else {
            new Event(user, goal, "").logEvent();

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
        Log.info(String.format("Recieved Request: [GET] SCHEDULE - first = %s & last - %s & email - %s", firstName, lastName, email));


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
    public Response hostSelect(@QueryParam("id") final String id) {
        Log.info(String.format("Recieved Request: [GET] PROBLEMSELECT - id = %s", id));

        User user = User.getUser(Integer.parseInt(id));

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("REDID", Integer.toString(user.id));
        params.put("FIRST", user.firstName);

        return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.MEET_ID, params)).build();
    }
}