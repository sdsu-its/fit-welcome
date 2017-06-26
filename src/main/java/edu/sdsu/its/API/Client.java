package edu.sdsu.its.API;

import com.google.gson.Gson;
import edu.sdsu.its.API.Models.Staff;
import edu.sdsu.its.API.Models.User;
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
    private static final Gson GSON = new Gson();

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
        LOGGER.info("Recieved Request: [GET] LOGIN - id = " + uid);

        int id = User.parseSwipe(uid);
        Staff staff = Staff.getStaff(id);
        User user = (staff == null) ? User.getUser(id) : null;

        if (user == null && staff == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(GSON.toJson(new Web.SimpleMessage("User not Found"))).build();
        }

        return Response.status(Response.Status.OK).entity(GSON.toJson(staff == null ? user : staff)).build();
    }
}
