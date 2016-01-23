package edu.sdsu.its.fit_welcome;

import edu.sdsu.its.fit_welcome.Models.User;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * FollowUp is an additional module that Sends Follow Up emails after Users visit the FIT Center.
 * These endpoints allow the user to unsubscribe from emails that they receive as a result of Followup.
 *
 * @author Tom Paulus
 *         Created on 1/3/16.
 */
@Path("followup")
public class Followup {
    /**
     * In order to comply with CAN-SPAM Regulations, all emails must contain an unsubscribe link.
     * Update the Database to reflect this request.
     *
     * @param email {@link String} Email to be unsubscribed
     * @return {@link Response} Web Response, including HTML Page. Returns 404 if no email is provided.
     */
    @Path("unsubscribe")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response unsubscribe(@QueryParam("e") final String email) {
        Response response;
        User user = null;

        if (email != null && email.length() < 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (email != null) {
            user = DB.getUser(email);
        }

        if (email != null && user != null) {
            HashMap<String, String> params = new HashMap<>();
            params.put("FNAME", user.firstName);
            params.put("LNAME", user.lastName);

            DB.unsubscribe(email);
            response = Response.status(Response.Status.OK).entity(Pages.makePage(Pages.UNSUBSCRIBE, params)).build();
        } else {
            response = Response.status(Response.Status.NOT_FOUND).build();
        }

        return response;
    }

    /**
     * Endpoint to which the resubscribe form is POSTed on the Unsubscribe page in case the user accidentally unsubscribed.
     *
     * @param m {@link String} URL-Encoded Form
     * @return {@link Response} Web Response, includes HTML welcome back page.
     */
    @Path("subscribe")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response subscribe(final String m) {
        String email;
        try {
            email = java.net.URLDecoder.decode(m, "utf-8");
            email = email.replace("email=", "");
        } catch (UnsupportedEncodingException e) {
            email = null;
            Logger.getLogger(getClass()).info("Request to Subscribe contained invalid formatting.", e);
        }

        Response response;
        User user = null;

        if (email != null) {
            user = DB.getUser(email);
        }

        if (email != null && user != null) {
            HashMap<String, String> params = new HashMap<>();
            params.put("FNAME", user.firstName);

            DB.subscribe(email);
            response = Response.status(Response.Status.OK).entity(Pages.makePage(Pages.SUBSCRIBE, params)).build();
        } else {
            response = Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }

        return response;
    }

}
