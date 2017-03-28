package edu.sdsu.its.fit_welcome;

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


/**
 * Manage Locales, which allow different menus to be presented at different locations
 *
 * @author Tom Paulus
 *         Created on 12/16/16.
 */
@Path("locales")
public class Locales {
    private static final Logger LOGGER = Logger.getLogger(Locales.class);

    /**
     * Get all Locales for which events have been recorded
     *
     * @return {@link Response} Json Array of Locales
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listLocales() {
        LOGGER.info("Received Request: [GET] LOCALES");
        Gson gson = new Gson();
        List locales = DB.getLocales();
        LOGGER.debug(String.format("Found %d locales", locales.size()));
        return Response.status(Response.Status.OK).entity(gson.toJson(locales)).build();
    }
}
