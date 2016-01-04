package edu.sdsu.its.fit_welcome.followup.Models;

import edu.sdsu.its.fit_welcome.followup.DB;

/**
 * Models a User
 *
 * @author Tom Paulus
 *         Created on 1/3/16.
 */
public class User {
    public int id;
    public String firstName;
    public String lastName;
    public String email;
    public boolean subscribed;

    public User(final int id, final String firstName, final String lastName, final String email, final boolean subscribed) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.subscribed = subscribed;
    }

    public static User getUser(final int id) {
        return DB.getUser(id);
    }
}