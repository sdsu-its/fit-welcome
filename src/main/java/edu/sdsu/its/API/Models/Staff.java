package edu.sdsu.its.API.Models;

import edu.sdsu.its.Welcome.DB;

/**
 * Models a Staff User
 *  - If a staff user exists, it overrides the nornal User that may exists for that id.
 *
 * @author Tom Paulus
 *         Created on 12/15/15.
 */
public class Staff extends User {
    public boolean clockable;
    public boolean admin;
    public boolean instructional_designer;

    public Staff(int id, String firstName, String lastName, String email, boolean clockable, boolean admin, boolean instructional_designer) {
        super(id, firstName, lastName, email, false);
        this.clockable = clockable;
        this.admin = admin;
        this.instructional_designer = instructional_designer;
    }

    public static Staff getStaff(final int id) {
        return DB.getStaff(id);
    }

    public static Staff getStaff(final String id) {
        return DB.getStaff(parseSwipe(id));
    }

    public static Staff[] getAllStaff(final String restriction) {
        return DB.getAllStaff(restriction == null ? "" : restriction);
    }


}
