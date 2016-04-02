package edu.sdsu.its.fit_welcome;

import com.google.gson.Gson;
import edu.sdsu.its.fit_welcome.Models.Event;
import edu.sdsu.its.fit_welcome.Models.Staff;
import edu.sdsu.its.fit_welcome.Models.TimeEntry;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin Interfaces
 *
 * @author Tom Paulus
 *         Created on 1/1/16.
 */
@Path("admin")
public class Admin {
    private static Logger Log = Logger.getLogger(Admin.class);
    private static Gson GSON = new Gson();

    /**
     * Get a list of all Clockable Staff
     * Used by the Manual Clock In/Out Admin Page
     *
     * @param requester {@link String} Admin's ID
     * @return {@link Response} List of all Clockable Staff
     */
    @Path("clockableStaff")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClockableStaff(@HeaderParam("REQUESTER") final String requester) {
        Log.info("Recieved Request: [GET] CLOCKABLESTAFF - Header:Requester - " + requester);
        Staff staff = (requester != null && requester.length() > 0) ? Staff.getStaff(Integer.parseInt(requester)) : null;
        if (staff == null || !staff.admin) {
            Log.warn("Unauthorized Request to CLOCKABLESTAFF - ID: " + requester);
            return Response.status(Response.Status.FORBIDDEN).entity(GSON.toJson(new Web.SimpleMessage("ID is not a valid Admin ID"))).build();
        }

        Staff[] clockableStaff = DB.getAllStaff("WHERE clockable = 1");
        return Response.status(Response.Status.OK).entity(GSON.toJson(clockableStaff)).build();
    }

    /**
     * Adds a Time Entry for a Staff User
     *
     * @param requester {@link String} Admin's ID
     * @param payload   {@link String} {@see Models.TimeEntry} TimeEntry JSON
     * @return {@link Response}
     */
    @Path("timeEntry")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addTimeEntry(@HeaderParam("REQUESTER") final String requester, final String payload) {
        Log.info("Recieved Request: [POST] TIMEENTRY - Header:Requester - " + requester + " & Payload: " + payload);
        Staff staff = (requester != null && requester.length() > 0) ? Staff.getStaff(Integer.parseInt(requester)) : null;
        if (staff == null || !staff.admin) {
            Log.warn("Unauthorized Request to TIMEENTRY - ID: " + requester);
            return Response.status(Response.Status.FORBIDDEN).entity(GSON.toJson(new Web.SimpleMessage("ID is not a valid Admin ID"))).build();
        }

        TimeEntry entry = GSON.fromJson(payload, TimeEntry.class);

        if (entry.direction) { // Clock In
            Log.info(String.format("Adding Manual Clock In Entry for %d at %s", entry.user.id, entry.getDate()));
            DB.clockIn(entry.user.id, "STR_TO_DATE('" + entry.getDate() + "','%Y-%m-%dT%H:%i')");
        } else { // Clock Out
            Log.info(String.format("Adding Manual Clock Out Entry for %d at %s", entry.user.id, entry.getDate()));
            DB.clockOut(entry.user.id, "STR_TO_DATE('" + entry.getDate() + "','%Y-%m-%dT%H:%i')");
        }

        return Response.status(Response.Status.CREATED).entity(GSON.toJson(new Web.SimpleMessage("Clock Record Added Successfully"))).build();
    }


    /**
     * Clock Out all Staff Users who are currently clocked in
     *
     * @param requester {@link String} Admin's ID
     * @return {@link Response} Status Message
     */
    @Path("clockOutAll")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response clockOutAll(@HeaderParam("REQUESTER") final String requester) {
        Log.info("Recieved Request: [POST] CLOCKOUTALL - Header:Requester - " + requester);
        Staff staff = (requester != null && requester.length() > 0) ? Staff.getStaff(Integer.parseInt(requester)) : null;
        if (staff == null || !staff.admin) {
            Log.warn("Unauthorized Request to CLOCKOUTALL - ID: " + requester);
            return Response.status(Response.Status.FORBIDDEN).entity(GSON.toJson(new Web.SimpleMessage("ID is not a valid Admin ID"))).build();
        }

        for (Staff s : DB.getAllStaff("WHERE clockable = 1")) {
            Clock clock = new Clock(s);
            if (clock.getStatus()) {
                Log.info(String.format("Clocking Out %s", s.firstName));
                clock.toggle();
            } else {
                Log.debug(String.format("Skipping %s, already clocked out", s.firstName));
            }
        }

        return Response.status(Response.Status.ACCEPTED).entity(GSON.toJson(new Web.SimpleMessage("All Users have been Clocked Out"))).build();
    }

    /**
     * Run a TimeSheet Report.
     * Reports are emailed to the respective parties once they have been generated.
     *
     * @param requester  {@link String} Admin's ID
     * @param startDate  {@link String} HTML date-local formatted date for Start of Report
     * @param endDate    {@link String} HTML date-local formatted date for End of Report
     * @param individual {@link Boolean} If the Report should be sent to each staff member
     * @return {@link Response} Status Message
     */
    @Path("timesheetReport")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response timesheetReport(@HeaderParam("REQUESTER") final String requester, @QueryParam("startDate") final String startDate, @QueryParam("endDate") final String endDate, @QueryParam("individual") final Boolean individual) {
        Log.info(String.format("Recieved Request: [GET] TIMESHEETREPORT - Header:Requester - %s & startDate - %s & endDat - %s & individual - %b", requester, startDate, endDate, individual));
        Staff staff = (requester != null && requester.length() > 0) ? Staff.getStaff(Integer.parseInt(requester)) : null;
        if (staff == null || !staff.admin) {
            Log.warn("Unauthorized Request to TIMESHEETREPORT - ID: " + requester);
            return Response.status(Response.Status.FORBIDDEN).entity(GSON.toJson(new Web.SimpleMessage("ID is not a valid Admin ID"))).build();
        }

        Report.TimesheetReport timesheetReport = new Report.TimesheetReport(Staff.getStaff(requester), startDate, endDate, individual);
        new Thread(timesheetReport).start();

        return Response.status(Response.Status.OK).entity(GSON.toJson(new Web.SimpleMessage("Timesheet Report Generated - Will be Emailed"))).build();
    }

    /**
     * Runs a Usage Report.
     * Reports are emailed to the respective parties once they have been generated.
     *
     * @param requester {@link String} Admin's ID
     * @param startDate {@link String} HTML date-local formatted date for Start of Report
     * @param endDate   {@link String} HTML date-local formatted date for End of Report
     * @return {@link Response} Status Message
     */
    @Path("usageReport")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response usageReport(@HeaderParam("REQUESTER") final String requester, @QueryParam("startDate") final String startDate, @QueryParam("endDate") final String endDate) {
        Log.info(String.format("Recieved Request: [GET] USAGEREPORT - Header:Requester - %s & startDate - %s & endDat - %s", requester, startDate, endDate));
        Staff staff = (requester != null && requester.length() > 0) ? Staff.getStaff(Integer.parseInt(requester)) : null;
        if (staff == null || !staff.admin) {
            Log.warn("Unauthorized Request to USAGEREPORT - ID: " + requester);
            return Response.status(Response.Status.FORBIDDEN).entity(GSON.toJson(new Web.SimpleMessage("ID is not a valid Admin ID"))).build();
        }

        Report.UsageReport usageReport = new Report.UsageReport(Staff.getStaff(requester), startDate, endDate);
        new Thread(usageReport).start();

        return Response.status(Response.Status.OK).entity(GSON.toJson(new Web.SimpleMessage("Usage Report Generated - Will be Emailed"))).build();
    }


    /**
     * Adds a Visit to the DB that has been entered after the visit occured
     *
     * @param requester {@link String} Admin's ID
     * @param payload   {@link String} {@see Models.Event} Event JSON
     * @return {@link Response} Status Message
     */
    @Path("manualVisit")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response manualVisit(@HeaderParam("REQUESTER") final String requester, final String payload) {
        Log.info(String.format("Recieved Request: [POST] MANUALVISIT - Header:Requester - %s & Payload - %s ", requester, payload));
        Staff staff = (requester != null && requester.length() > 0) ? Staff.getStaff(Integer.parseInt(requester)) : null;
        if (staff == null || !staff.admin) {
            Log.warn("Unauthorized Request to MANUALVISIT - ID: " + requester);
            return Response.status(Response.Status.FORBIDDEN).entity(GSON.toJson(new Web.SimpleMessage("ID is not a valid Admin ID"))).build();
        }

        Event event = GSON.fromJson(payload, Event.class); // Make Sure to set timeString
        event.params = (event.params == null || event.params.length() == 0) ? "Back Dated" : event.params + ", Back Dated"; // Add Back Dated Note to Event, without overwriting existing data
        final String pattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}";

        final Pattern r = Pattern.compile(pattern);
        final Matcher m = r.matcher(event.timeString);
        if (!m.find()) {
            Log.warn("Problem matching time to RegEx Format");
            Log.debug("Input Date/Time - " + event.timeString);
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(GSON.toJson(new Web.SimpleMessage("Error: Improperly Formatted Date String"))).build();
        }

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
        DateTime dt = formatter.parseDateTime(m.group().replace("T", " ")); //Remove Stupid T in HTML DateTime-Locale time
        event.timeString = new Timestamp(dt.getMillis()).toString();

        event.logEvent();
        return Response.status(Response.Status.CREATED).entity(GSON.toJson(new Web.SimpleMessage("Event Created and Logged Successfully"))).build();
    }

    /**
     * Adds a new Staff Member to the system
     *
     * @param requester {@link String} Admin's ID
     * @param payload   {@link String} {@see Models.Staff} Staff JSON
     * @return {@link Response} Status Message
     */
    @Path("newStaff")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response newStaff(@HeaderParam("REQUESTER") final String requester, final String payload) {
        Log.info(String.format("Recieved Request: [POST] NEWSTAFF - Header:Requester - %s & Payload - %s ", requester, payload));
        Staff staff = (requester != null && requester.length() > 0) ? Staff.getStaff(Integer.parseInt(requester)) : null;
        if (staff == null || !staff.admin) {
            Log.warn("Unauthorized Request to NEWSTAFF - ID: " + requester);
            return Response.status(Response.Status.FORBIDDEN).entity(GSON.toJson(new Web.SimpleMessage("ID is not a valid Admin ID"))).build();
        }

        Staff newStaff = GSON.fromJson(payload, Staff.class);
        if (!DB.createNewStaff(newStaff)) {
            Log.warn(String.format("Cannot Create Staff Member (%s %s) ID already exists in DB", newStaff.firstName, newStaff.lastName));
            return Response.status(Response.Status.BAD_REQUEST).entity(GSON.toJson(new Web.SimpleMessage("ID Already Exists for a Staff Member"))).build();
        } else {
            return Response.status(Response.Status.CREATED).entity(GSON.toJson(new Web.SimpleMessage("Staff Member Created"))).build();
        }
    }
}
