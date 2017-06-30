package edu.sdsu.its.API;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import edu.sdsu.its.API.Models.Event;
import edu.sdsu.its.API.Models.SimpleMessage;
import edu.sdsu.its.API.Models.Staff;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.Welcome.DB;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

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
            return Response.status(Response.Status.NOT_FOUND).entity(GSON.toJson(new SimpleMessage("User not Found"))).build();
        }

        return Response.status(Response.Status.OK).entity(GSON.toJson(staff == null ? user : staff)).build();
    }

    @Path("appointments")
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upcomingAppointments() {
        Acutiy.Appointment[] appointments = Acutiy.getUpcoming();
        LOGGER.info(String.format("There are %d upcoming appointments", appointments.length));

        RedactedAppointment[] result = new RedactedAppointment[appointments.length];
        for (int i = 0; i < appointments.length; i++) {
            result[i] = new RedactedAppointment(appointments[i]);
        }

        return Response.status(Response.Status.OK).entity(GSON.toJson(result)).build();
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
        final Event event = new Gson().fromJson(payload, ClientEvent.class).convertToEvent();

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

    private static class ClientEvent {
        boolean appointmentMade;
        Integer appointmentId;
        User owner;
        String goal;
        String params;
        String locale;

        Event convertToEvent() {
            if ((this.appointmentId == null || new Integer(0).equals(this.appointmentId)) && this.appointmentMade) {
                // Check for "Lost" Appointment Existence
                final Acutiy.Appointment apptByOwner = Acutiy.getApptByOwner(this.owner.id);
                if (apptByOwner != null) {
                    this.appointmentId = apptByOwner.id;
                    LOGGER.info("Found matching appointment for user that was not shown in UI");
                    LOGGER.debug("Appointment ID: " + this.appointmentId);
                    LOGGER.debug("Owner: " + this.owner.id);
                }
            }

            if (this.appointmentId != null && !new Integer(0).equals(this.appointmentId)) {
                // Process Appointment Check-in
                LOGGER.debug(String.format("Checking Appointment with ID: %d", this.appointmentId));
                Acutiy.Appointment appointment = Acutiy.checkIn(this.appointmentId);
                Acutiy.AppointmentType appointmentType = DB.getAppointmentTypeMatch(new Acutiy.AppointmentType(appointment.appointmentTypeID));

                if (owner == null || owner.id == 0) {
                    final int ownerID = appointment.getOwnerID();

                    if (owner == null) {
                        owner = new User(ownerID);
                    } else {
                        owner.id = ownerID;
                    }
                }
                LOGGER.debug(String.format("Appointment Owner: %d", owner.id));
                this.goal = appointmentType.eventText;
                if (this.params != null && !this.params.isEmpty()) {
                    this.params = this.params + ", " + "Appointment ID: " + this.appointmentId;
                }

                if (appointmentType.eventParams != null && appointmentType.eventParams.length() > 0)
                    this.params = appointmentType.eventParams + ", " + this.params;
            } else {
                LOGGER.debug("This is a Walk-in Appointment");
            }

            return new Event(owner, goal, params, locale);
        }
    }

    private static class RedactedAppointment {
        private static final int ID_LENGTH = 9;
        private static final int REVEAL = 3;
        private static final String MASK_CHAR = "*";

        @Expose Integer id;
        @Expose String time;
        @Expose String name;
        @Expose String type;
        @Expose String ownerId;

        public RedactedAppointment(Acutiy.Appointment appointment) {
            this.id = appointment.id;
            this.time = appointment.time;
            this.name = String.format("%s, %s.", appointment.lastName, appointment.firstName.charAt(0));
            this.type = appointment.type;
            this.ownerId = String.join("", Collections.nCopies(ID_LENGTH - REVEAL, MASK_CHAR))
                    + appointment.getOwnerID() % ((int) Math.pow(10, REVEAL));
        }


    }
}
