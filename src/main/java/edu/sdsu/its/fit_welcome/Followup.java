package edu.sdsu.its.fit_welcome;

import com.google.gson.Gson;
import edu.sdsu.its.fit_welcome.Models.User;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * FollowUp is an additional module that Sends Follow Up emails after Users visit the FIT Center.
 * These endpoints allow the user to unsubscribe from emails that they receive as a result of Followup.
 *
 * @author Tom Paulus
 *         Created on 1/3/16.
 */
@Path("followup")
public class Followup {
    private static final Logger LOGGER = Logger.getLogger(Followup.class);
    private static final Gson GSON = new Gson();

    /**
     * In order to comply with CAN-SPAM Regulations, all emails must contain an unsubscribe link.
     * Update the Database to reflect this request.
     *
     * @param email {@link String} Email to be unsubscribed
     * @return {@link Response} {@see Models.User} User associated to the provided email
     */
    @Path("unsubscribe")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unsubscribe(@QueryParam("email") final String email) {
        LOGGER.info("Recieved Request: [GET] FOLLOWUP/UNSUBSCRIBE - email = " + email);
        Response response;
        User user = null;

        if (email != null && email.length() < 0) {
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(GSON.toJson(new Web.SimpleMessage("No Email included in request"))).build();
        }

        if (email != null) {
            user = DB.getUser(email);
        }

        if (email != null && user != null) {
            LOGGER.debug(String.format("Matched User to Email - %s %s (ID: %d)", user.firstName, user.lastName, user.id));
            DB.unsubscribe(email);
            response = Response.status(Response.Status.ACCEPTED).entity(GSON.toJson(user)).build();
        } else {
            LOGGER.warn(String.format("No Matching User found for email \"%s\"", email));
            response = Response.status(Response.Status.NOT_FOUND).entity(GSON.toJson(new Web.SimpleMessage("User Not Found"))).build();
        }
        return response;
    }

    /**
     * Endpoint to which the resubscribe form is POSTed on the Unsubscribe page in case the user accidentally unsubscribed.
     *
     * @param email {@link String} Email Address to be Subscribed
     * @return {@link Response} {@see Models.User} User associated to the provided email
     */
    @Path("subscribe")
    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subscribe(@QueryParam("email") final String email) {
        LOGGER.info("Recieved Request: [GET] FOLLOWUP/SUBSCRIBE - email = " + email);
        Response response;
        User user = null;

        if (email != null && email.length() < 0) {
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(GSON.toJson(new Web.SimpleMessage("No Email included in request"))).build();
        }

        if (email != null) {
            user = DB.getUser(email);
        }

        if (email != null && user != null) {
            DB.subscribe(email);
            response = Response.status(Response.Status.ACCEPTED).entity(GSON.toJson(user)).build();
        } else {
            response = Response.status(Response.Status.NOT_FOUND).entity(GSON.toJson(new Web.SimpleMessage("User not Found"))).build();
        }

        return response;
    }

}
