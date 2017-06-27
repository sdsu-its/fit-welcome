package edu.sdsu.its.API;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import edu.sdsu.its.API.Models.Event;
import edu.sdsu.its.API.Models.Staff;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.Welcome.DB;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Tom Paulus
 *         Created on 6/25/17.
 */
@Path("client")
public class Client {
    private static final Logger LOGGER = Logger.getLogger(Client.class);
    private static Gson GSON = null;

    public Client() {
        if (GSON == null) {
            // Only Items tagged with @Expose are okay for Public/Non-Authenticated Info
            GsonBuilder builder = new GsonBuilder();
            builder.excludeFieldsWithoutExposeAnnotation();
            GSON = builder.create();
        }
    }

    /**
     * Login a User/Staff member to the Client UI via their ID
     *
     * @param uid {@link String} User ID String (Either Raw ID or Swipe String)
     * @return {@link User} Corresponding User, 404 if not found
     */
    @Path("login")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@QueryParam("id") String uid) {
        uid = uid.replace(" ", "+"); // Re Encode Spaces as + signs
        LOGGER.info("Received Request: [GET] LOGIN - id = " + uid);

        int id = User.parseSwipe(uid);
        Staff staff = Staff.getStaff(id);
        User user = (staff == null) ? User.getUser(id) : null;

        if (user == null && staff == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(GSON.toJson(new Web.SimpleMessage("User not Found"))).build();
        }

        return Response.status(Response.Status.OK).entity(GSON.toJson(staff == null ? user : staff)).build();
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
        LOGGER.info("Received Request: [POST] EVENT - " + payload);

        // Use a Standard JSON to de-marshal the data, since we want all fields
        final Event event = new Gson().fromJson(payload, Event.class);

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
        @Expose String message;

        public SimpleMessage(String message) {
            this.message = message;
        }
    }
}
