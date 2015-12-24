package edu.sdsu.its.fit_welcome.Models;

import edu.sdsu.its.fit_welcome.DB;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Models a standard User
 * - These users see only the standard welcome page.
 *
 * @author Tom Paulus
 *         Created on 12/12/15.
 */
public class User {
    public int id;
    public String firstName;
    public String lastName;
    public String email;

    public User(int id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public static User getUser(final int id) {
        return DB.getUser(id);
    }

    public static User getUser(final String id) {
        return DB.getUser(parseSwipe(id));
    }

    /**
     * Parse the RedID out of the string that is produced when an SDSUCard is swiped.
     * Also works if the ID has been typed in manually.
     *
     * @param swipe {@link String} Swiped/Typed RedID
     * @return {@link int} User's RedID
     */
    public static int parseSwipe(final String swipe) {
        int id = 0;

        String pattern = "(^\\d{9}$)|\\+(\\d{9})=";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(swipe);
        if (m.find()) {
            if (m.group(0) != null) {
                id = Integer.parseInt(m.group(0).replace("+", "").replace("=", "")); // Remove MagStrip Encoding
            } else {
                id = Integer.parseInt(m.group(1));
            }
        }
        return id;
    }
}
