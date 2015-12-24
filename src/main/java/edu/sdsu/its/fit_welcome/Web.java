package edu.sdsu.its.fit_welcome;

import edu.sdsu.its.fit_welcome.Models.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
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
    /**
     * Welcome Page
     *
     * @param uid         {@link String} UserID (Either typed in or Magstripe Encoded)
     * @param skipAcuity {@link String} if "yes" Acuity will not be checked for appointments
     * @return {@link Response} Response
     */
    @Path("welcome")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response welcome(@QueryParam("id") final String uid, @QueryParam("skip_sch") final String skipAcuity) {
        Logger.getLogger(getClass()).info(String.format("Recieved Request: [GET] WELCOME - id = %s & skip_sch - %s", uid, skipAcuity));

        int redid = User.parseSwipe(uid);

        User user = User.getUser(redid);
        Staff staff = Staff.getStaff(redid);

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
            params.put("ADMIN", staff.admin ? "" : "disabled=\"disabled\"");

            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.STAFF_WELCOME, params)).build();
        } else if (staff != null && staff.admin) {
            try {
                final URI redirect = new URIBuilder()
                        .setPath("admin")
                        .setParameter("id", Integer.toString(redid))
                        .build();

                return Response.seeOther(redirect).build();
            } catch (URISyntaxException e) {
                Logger.getLogger(getClass()).warn("Problem Creating Redirect URI", e);
            }
        }

        Appointment appointment = !"yes".equals(skipAcuity) ? Acutiy.getAppt(user) : null;
        if (appointment != null) {
            params.put("TIME", appointment.time);
            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.APPT_FOUND, params)).build();
        }

        return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.WELCOME, params)).build();
    }

    /**
     * Confirmation Page
     *
     * @param id      {@link String} User's ID
     * @param goal    {@link String} User's Goal
     * @param hasAppt {@link String} If an Acuity Appointment was found.
     * @param source  {@link String} Which page the user came from
     * @param hostId  {@link String} ID of their Host if they have a meeting
     * @return {@link Response} Response
     */
    @Path("conf")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response confirmation(@QueryParam("id") final String id, @QueryParam("goal") final String goal,
                                 @QueryParam("has_appt") final String hasAppt, @QueryParam("source") final String source,
                                 @QueryParam("host") final String hostId) {

        Logger.getLogger(getClass()).info(String.format("Recieved Request: [GET] CONF - id = %s & goal - %s & has_appt - %s & source - %s & host - %s", id, goal, hasAppt, source, hostId));

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
                Logger.getLogger(getClass()).warn("Problem Creating Redirect URI", e);
            }
        } else if ("Meet an ID".equals(goal)) {
            if (hostId == null) {
                try {
                    final URI redirect = new URIBuilder()
                            .setPath("hostSelect")
                            .setParameter("id", Integer.toString(user.id))
                            .build();

                    return Response.seeOther(redirect).build();
                } catch (URISyntaxException e) {
                    Logger.getLogger(getClass()).warn("Problem Creating Redirect URI", e);
                }
            } else {
                if ("bolt".equals(hostId)) {
                    new Event(user, goal, "BOLT/QOLT Consult").logEvent();
                } else {
                    final Staff host = Staff.getStaff(hostId);
                    new Event(user, goal, host.firstName).logEvent();

                    new Thread() {
                        @Override
                        public void run() {
                            Logger.getLogger(getClass()).info("Starting new Thread to send Notification Email");
                            new SendEmail().emailNotification(host, user).send(host.email);
                        }
                    }.start();
                }
                return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.CONFIRMATION, params)).build();
            }
        } else if ("appt_found".equals(source) && "no".equals(hasAppt.toLowerCase())) {
            try {
                final URI redirect = new URIBuilder()
                        .setPath("welcome")
                        .setParameter("id", id)
                        .setParameter("skip_sch", "yes")
                        .build();

                return Response.seeOther(redirect).build();
            } catch (URISyntaxException e) {
                Logger.getLogger(getClass()).warn("Problem Creating Redirect URI", e);
            }
        } else {
            new Event(user, goal, "yes".equals(hasAppt.toLowerCase()) ? "appt match found" : "").logEvent();
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
        Logger.getLogger(getClass()).info(String.format("Recieved Request: [GET] SCHEDULE - first = %s & last - %s & email - %s", firstName, lastName, email));


        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("FIRST", firstName);
        params.put("LAST", lastName);
        params.put("EMAIL", email);

        return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.SCHEDULE, params)).build();
    }

    /**
     * Host Selection Page
     *
     * @param id {@link String} User's ID
     * @return {@link Response} Response
     */
    @Path("hostSelect")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response hostSelect(@QueryParam("id") final String id) {
        Logger.getLogger(getClass()).info(String.format("Recieved Request: [GET] HOSTSELECT - id = %s", id));

        User user = User.getUser(Integer.parseInt(id));

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("REDID", Integer.toString(user.id));
        params.put("FIRST", user.firstName);
        params.put("HOSTS", Pages.arrayToButtons(Staff.getAllStaff("WHERE instructional_designer = 1")));

        return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.MEET_ID, params)).build();
    }

    /**
     * Admin Page
     *
     * @param id          {@link String} User's ID
     * @param adminAction {@link String} Requested Action. If null options page is displayed.
     * @return {@link Response} Response
     */
    @Path("admin")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response admin(@QueryParam("id") final String id, @QueryParam("action") final String adminAction) {
        Logger.getLogger(getClass()).info(String.format("Recieved Request: [GET] ADMIN - id = %s & action - %s", id, adminAction));

        Staff staff = id != null ? Staff.getStaff(Integer.parseInt(id)) : null;
        if (staff == null || !staff.admin) {
            return Response.status(Response.Status.FORBIDDEN).entity(Pages.makePage(Pages.FORBIDDEN, new HashMap<String, String>())).build();
        }

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("REDID", id);

        if (adminAction == null) {
            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.ADMIN, params)).build();
        }

        if ("manual time".equals(adminAction)) {
            params.put("STAFFUSERS", Pages.arrayToList(Staff.getAllStaff("WHERE clockable = 1")));
            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.MANUAL_TIME, params)).build();
        }

        if ("email timesheets".equals(adminAction)) {
            params.put("REPORT", "Staff Timesheets");
            params.put("RTYPE", "timesheets");
            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.REPORT_DATE_PICKER, params)).build();
        }

        if ("new staff".equals(adminAction)) {
            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.NEW_USER, params)).build();
        }

        if ("run report".equals(adminAction)) {
            params.put("REPORT", "Usage Report");
            params.put("RTYPE", "usage");
            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.REPORT_DATE_PICKER, params)).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Request").build();

    }

    /**
     * Reports Pages
     *
     * @param id         {@link String} User's ID
     * @param reportType {@link String} Which Type of report to Run.
     *                   Either 'timesheets' or 'usage'
     * @param startDate  {@link String} Start Date for the Report
     * @param endDate    {@link String} End Date for the Report
     * @return {@link Response} Response
     */
    @Path("admin/report")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response report(@QueryParam("id") final String id, @QueryParam("report_type") final String reportType, @QueryParam("start") final String startDate, @QueryParam("end") final String endDate) {
        Logger.getLogger(getClass()).info(String.format("Recieved Request: [GET] ADMIN/REPORT - id = %s & report_type - %s & start - %s & end - %s", id, reportType, startDate, endDate));

        final Staff staff = id != null ? Staff.getStaff(Integer.parseInt(id)) : null;
        if (staff == null || !staff.admin) {
            return Response.status(Response.Status.FORBIDDEN).entity(Pages.makePage(Pages.FORBIDDEN, new HashMap<String, String>())).build();
        }

        final Quote quote = Quote.getRandom();

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("FIRST", staff.firstName);
        params.put("QUOTE", quote.text);
        params.put("QUOTEAUTHOR", quote.author);

        if ("timesheets".equals(reportType)) {
            new Thread() {
                @Override
                public void run() {
                    Logger.getLogger(getClass()).info("Starting new Thread to Generate Timesheets");

                    final Staff[] allClockableStaff = DB.getAllStaff("WHERE clockable = 1");
                    File[] timesheets = new File[allClockableStaff.length];
                    for (int s = 0; s < allClockableStaff.length; s++) {
                        final int staffId = allClockableStaff[s].id;
                        final String staffLast = allClockableStaff[s].lastName;
                        timesheets[s] = Timesheet.make(DB.exportClockIOs(staffId, startDate, endDate), staffLast);
                    }

                    new SendEmail().emailFile("Staff Timesheets", staff.firstName, timesheets).send(staff.email);
                }
            }.start();

            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.REPORT_CONF, params)).build();
        }

        if ("usage".equals(reportType)) {
            new Thread() {
                @Override
                public void run() {
                    Logger.getLogger(getClass()).info("Starting new Thread to Generate Usage Report");

                    new SendEmail().emailFile("Events Report", staff.firstName, new File[]{DB.exportEvents(startDate, endDate, "events")}).send(staff.email);
                }
            }.start();

            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.REPORT_CONF, params)).build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Request").build();
    }

    /**
     * Manual Time Entry Page
     *
     * @param id     {@link String} User's ID
     * @param userID {@link String} ID of user who will be clocked in/out
     * @param action {@link String} Action to be performed.
     *               Either: 'clockIn' or 'clockOut'
     * @param date   {@link String} Date and Time the action should be performed
     *               In HTML datetime-locale format
     * @return {@link Response} Response
     */
    @Path("admin/manual_time")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response manualTime(@QueryParam("id") final String id, @QueryParam("user") final String userID, @QueryParam("action") final String action, @QueryParam("date") final String date) {
        Logger.getLogger(getClass()).info(String.format("Recieved Request: [GET] ADMIN/MANUAL_TIME - id = %s & userID - %s & action - %s & date - %s", id, userID, action, date));

        Staff staff = id != null ? Staff.getStaff(Integer.parseInt(id)) : null;
        if (staff == null || !staff.admin) {
            return Response.status(Response.Status.FORBIDDEN).entity(Pages.makePage(Pages.FORBIDDEN, new HashMap<String, String>())).build();
        }

        final Quote quote = Quote.getRandom();

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("FIRST", staff.firstName);
        params.put("QUOTE", quote.text);
        params.put("QUOTEAUTHOR", quote.author);

        final Staff staff1 = Staff.getStaff(userID);

        if ("clockIn".equals(action)) {
            DB.clockIn(Integer.parseInt(userID), "STR_TO_DATE('" + date + "','%Y-%m-%dT%H:%i')");
            params.put("ACTION", String.format("Manually Clocked In %s %s", staff1.firstName, staff1.lastName));
        } else if ("clockOut".equals(action)) {
            DB.clockOut(Integer.parseInt(userID), "STR_TO_DATE('" + date + "','%Y-%m-%dT%H:%i')");
            params.put("ACTION", String.format("Manually Clocked Out %s %s", staff1.firstName, staff1.lastName));
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Request").build();
        }

        return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.ADMIN_CONF, params)).build();
    }

    /**
     * New Staff User Creation Page
     *
     * @param id                     {@link String} User's ID
     * @param userID                 {@link String} New User's ID
     * @param userFirst              {@link String} New User's First Name
     * @param userLast               {@link String} New User's Last Name
     * @param email                  {@link String} New User's Email Address
     * @param clockable              {@link String}
     *                               Either: '0' or '1'
     * @param admin                  {@link String}
     *                               Either: '0' or '1'
     * @param instructional_designer {@link String}
     *                               Either: '0' or '1'
     * @return {@link Response} Response
     */
    @Path("admin/create_user")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_HTML)
    public Response createUser(@QueryParam("id") final String id, @QueryParam("user_id") final String userID, @QueryParam("user_first") final String userFirst,
                               @QueryParam("user_last") final String userLast, @QueryParam("email") final String email, @QueryParam("clockable") final String clockable, @QueryParam("admin") final String admin,
                               @QueryParam("instructional_designer") final String instructional_designer) {
        Logger.getLogger(getClass()).info(String.format("Recieved Request: [GET] ADMIN/CREATE_USER - id = %s & user_id - %s & user_first - %s & user_last - %s & clockable - %s & admin - %s", id, userID, userFirst, userLast, clockable, admin));

        final Staff staff = id != null ? Staff.getStaff(Integer.parseInt(id)) : null;
        if (staff == null || !staff.admin) {
            return Response.status(Response.Status.FORBIDDEN).entity(Pages.makePage(Pages.FORBIDDEN, new HashMap<String, String>())).build();
        }

        final Quote quote = Quote.getRandom();

        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("FIRST", staff.firstName);
        params.put("QUOTE", quote.text);
        params.put("QUOTEAUTHOR", quote.author);

        final Staff staff1 = new Staff(Integer.parseInt(userID), userFirst, userLast, email, clockable.equals("1"), admin.equals("1"), instructional_designer.equals("1"));
        if (!DB.createNewStaff(staff1)) {
            params.put("ACTION", "Entered an ID that already exists");
            return Response.status(Response.Status.BAD_REQUEST).entity(Pages.makePage(Pages.ADMIN_CONF, params)).build();
        } else {
            params.put("ACTION", "successfully created a new Staff user");
            return Response.status(Response.Status.OK).entity(Pages.makePage(Pages.ADMIN_CONF, params)).build();
        }
    }
}