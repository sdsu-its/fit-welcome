package edu.sdsu.its.API.Models;

import edu.sdsu.its.API.Acutiy;

/**
 * Models a Login for a User
 * Includes their designation (user vs. staff), their user information, and their appointment id they have one.
 *
 * @author Tom Paulus
 *         Created on 3/28/16.
 */
public class Login {
    public User user;
    public boolean isStaff;
    public Acutiy.Appointment appointment;

    public Login(User user, boolean isStaff, Acutiy.Appointment appointment) {
        this.user = user;
        this.isStaff = isStaff;
        this.appointment = appointment;
    }
}
