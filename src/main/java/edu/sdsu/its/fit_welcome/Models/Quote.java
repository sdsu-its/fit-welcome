package edu.sdsu.its.fit_welcome.Models;

import edu.sdsu.its.fit_welcome.DB;

import java.util.Random;

/**
 * Models a Quote
 *  - Displayed on most confirmation pages
 *
 * @author Tom Paulus
 *         Created on 12/15/15.
 */
public class Quote {
    public static Quote[] quotes = DB.getQuotes();

    public String text;
    public String author;

    public Quote(String text, String author) {
        this.text = text;
        this.author = author;
    }

    public static Quote getRandom() {
        return quotes[new Random().nextInt(quotes.length)];
    }
}
