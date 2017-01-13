package edu.sdsu.its.fit_welcome;

import com.google.gson.Gson;
import edu.sdsu.its.fit_welcome.Models.Event;
import edu.sdsu.its.fit_welcome.Models.Staff;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Live Dashboard
 * <p/>
 * Shows the current check-ins to staff users and updates live
 *
 * @author Tom Paulus
 *         Created on 1/8/16.
 */
@Singleton
@Path("live")
public class Live {
    private static Logger LOGGER = Logger.getLogger(Live.class);
    private static SseBroadcaster broadcaster = new SseBroadcaster();

    private static Event prepareEvent(Event event) {
        // Checks for notify
        // ID: 999999999 is used to designate unknown IDs

        event.completeOwner();
        if ("Meet an ID".equals(event.type) && event.owner.id != 999999999) {
            LOGGER.debug("Event's goal is to meet with ID, setting Notify to True");
            event.notify = true;
        } else if ("Use ParScore".equals(event.type) && "Walk In".equals(event.params) && event.owner.id != 999999999) {
            LOGGER.debug("Event is a ParScore Walk In, setting Notify to True");
            event.notify = true;
        }
        event.timeString = event.time.toString("hh:mm a");
        event.params = event.params != null ? event.params : "";

        return event;
    }

    public static void broadcastEvent(Event broadcastEvent) {
        Gson gson = new Gson();
        OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
        OutboundEvent event = eventBuilder.name("message")
                .mediaType(MediaType.TEXT_PLAIN_TYPE)
                .data(String.class, gson.toJson(prepareEvent(broadcastEvent)))
                .build();
        LOGGER.info(String.format("Broadcasting new Message to Clients - Event ID: %d", broadcastEvent.id));
        broadcaster.broadcast(event);
    }

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

        LOGGER.info(String.format("Recieved Request: [GET] GETEVENTS - id = %s & last = %d", id, lastEvent));
        if (lastEvent == null || lastEvent == 0) {
            lastEvent = DB.numEvents() - 10;
        }

        final List<Event> eventList = DB.getEventsSince(lastEvent);
        final Event[] results = new Event[eventList.size()];

        for (int e = 0; e < eventList.size(); e++) {
            Event event = eventList.get(e);
            results[e] = prepareEvent(event);
        }

        final Gson gson = new Gson();
        return Response.status(Response.Status.OK).entity(gson.toJson(results)).build();
    }

    @GET
    @Path("stream")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput listenToBroadcast() {
        final EventOutput eventOutput = new EventOutput();
        Live.broadcaster.add(eventOutput);
        return eventOutput;
    }
}
