package edu.sdsu.its.API;

/**
 * Display Daily Quotes on the Confirmation Page
 *
 * @author Tom Paulus
 * Created on 3/29/16.
 */

import com.google.gson.Gson;
import edu.sdsu.its.Welcome.DB;
import org.joda.time.DateTime;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("quote")
public class Quote {
    /**
     * Get the Quote of the Day
     *
     * @return {@link Response} Quote and Author as JSON
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQuote() {
        QuoteModel quote = DB.getQuote(getQuoteNum());
        final Gson gson = new Gson();
        return Response.status(Response.Status.OK).entity(gson.toJson(quote)).build();
    }

    private int getQuoteNum() {
        return (DateTime.now().dayOfYear().get() % DB.getNumQuotes()) + 1;
    }

    public static class QuoteModel {
        public String author;
        public String text;

        public QuoteModel(String author, String text) {
            this.author = author;
            this.text = text;
        }
    }
}

