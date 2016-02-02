package edu.sdsu.its.fit_welcome.alerts.Models;

/**
 * Models a Staff User
 *
 * @author Tom Paulus
 *         Created on 2/1/16.
 */
public class Staff {
    public int id;
    public String firstName;
    public String lastName;
    public String email;

    public Staff(final int id, final String firstName, final String lastName, final String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }
}
