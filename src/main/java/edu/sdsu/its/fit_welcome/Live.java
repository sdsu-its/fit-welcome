package edu.sdsu.its.fit_welcome;

import com.google.gson.Gson;
import edu.sdsu.its.fit_welcome.Models.Event;
import edu.sdsu.its.fit_welcome.Models.Staff;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;

/**
 * Live Dashboard
 * <p/>
 * Shows the current checkins to staff users and updates live
 *
 * @author Tom Paulus
 *         Created on 1/8/16.
 */
@Path("live")
public class Live {
    Logger Log = Logger.getLogger(Live.class);

    /**
     * Get all events since the last event provided
     *
     * @param lastEvent {@link Integer} EventID of the last Event Recieved
     * @return {@link Response} All events since as JSON Document
     */
    @GET
    @Path("getEvents")
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents(@QueryParam("id") String id, @QueryParam("last") Integer lastEvent) {
        Staff staff = id != null ? Staff.getStaff(id.replace(" ", "+")) : null;  //JavaScript Converts +s to Spaces, that's Bad!
        if (staff == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Log.info(String.format("Recieved Request: [GET] GETEVENTS - id = %s & last = %d", id, lastEvent));
        if (lastEvent == null || lastEvent == 0) {
            lastEvent = DB.numEvents() - 10;
        }

        final List<Event> eventList = DB.getEventsSince(lastEvent);
        final Event[] results = new Event[eventList.size()];

        for (int e = 0; e < eventList.size(); e++) {
            Event event = eventList.get(e);

            // Checks for notify
            // ID: 999999999 is used to designate unknown IDs
            if ("Meet an ID".equals(event.type) && event.owner.id != 999999999) {
                Log.debug("Event's goal is to meet with ID, setting Notify to True");
                event.notify = true;
            } else if ("Use ParScore".equals(event.type) && "Walk In".equals(event.params) && event.owner.id != 999999999) {
                Log.debug("Event is a ParScore Walk In, setting Notify to True");
                event.notify = true;
            }
            event.timeString = event.time.toString("hh:mm a");

            results[e] = event;
        }

        final Gson gson = new Gson();
        return Response.status(Response.Status.OK).entity(gson.toJson(results)).build();
    }
}
