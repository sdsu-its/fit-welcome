package edu.sdsu.its.API;

import com.google.gson.Gson;
import edu.sdsu.its.Welcome.DB;
import edu.sdsu.its.API.Models.Event;
import edu.sdsu.its.API.Models.Login;
import edu.sdsu.its.API.Models.Staff;
import edu.sdsu.its.API.Models.User;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    public Response login(@QueryParam("id") String uid) {
        uid = uid.replace(" ", "+"); // Re Encode Spaces as + signs
        LOGGER.info("Recieved Request: [GET] LOGIN - id = " + uid);

        int id = User.parseSwipe(uid);
        Staff staff = Staff.getStaff(id);
        User user = (staff == null) ? User.getUser(id) : null;

        if (user == null && staff == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(GSON.toJson(new SimpleMessage("User not Found"))).build();
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
            Response.status(Response.Status.NOT_ACCEPTABLE).entity(GSON.toJson(new SimpleMessage("ID does not have a Clock."))).build();
        }

        final boolean status = new Clock(staff).getStatus();
        return Response.status(Response.Status.OK).entity(status).build();
    }

    /**
     * Toggle the status of a Staff Member's Clock
     *
     * @param id {@link int} Staff Member's ID
     * @return {@link Response} True = User was Clocked In & False = User was Clocked Out
     */
    @Path("clock/toggle")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.TEXT_PLAIN)
    public Response toggleClock(@QueryParam("id") final int id) {
        LOGGER.info("Recieved Request: [GET] CLOCK/TOGGLE - id = " + id);

        Staff staff = Staff.getStaff(id);
        if (staff == null || !staff.clockable) {
            Response.status(Response.Status.NOT_ACCEPTABLE).entity(GSON.toJson(new SimpleMessage("ID does not have a Clock."))).build();
        }

        final boolean status = new Clock(staff).toggle();
        return Response.status(Response.Status.ACCEPTED).entity(status).build();
    }

    /**
     * Add a new Event (An event is when a Visitor Checks-In)
     *
     * @param payload {@link String} JSON Payload {@see Models.Event}
     * @return {@link Response} Completion Message
     */
    @Path("event")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addEvent(final String payload) {
        LOGGER.info("Recieved Request: [POST] EVENT - " + payload);

        final Event event = GSON.fromJson(payload, Event.class);

        if (event.params != null && event.params.contains("Appointment ID:")) {
            String appointmentID = event.params.replace("Appointment ID:", "").replaceAll(" ", "");
            LOGGER.debug(String.format("Checking In User: %d for appointment with ID: %s", event.owner.id, appointmentID));
            Acutiy.Appointment appointment = Acutiy.checkIn(Integer.parseInt(appointmentID));
            Acutiy.AppointmentType appointmentType = DB.getAppointmentTypeMatch(new Acutiy.AppointmentType(appointment.appointmentTypeID));

            event.type = appointmentType.eventText;
            if (appointmentType.eventParams != null && appointmentType.eventParams.length() > 0)
                event.params = appointmentType.eventParams + ", " + event.params;
        }


        Thread thread = new Thread() {
            public void run() {
                try {
                    LOGGER.debug("Broadcasting new event");
                    Live.broadcastEvent(event.logEvent());
                } catch (Exception e) {
                    LOGGER.warn("Problem Broadcasting Event", e);
                }
            }
        };
        thread.start();

        return Response.status(Response.Status.CREATED).entity(GSON.toJson(new SimpleMessage("Event Created and Logged Successfully"))).build();
    }

    public static class SimpleMessage {
        String message;

        public SimpleMessage(String message) {
            this.message = message;
        }
    }

}