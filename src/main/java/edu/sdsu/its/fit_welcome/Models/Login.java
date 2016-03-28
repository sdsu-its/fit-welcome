package edu.sdsu.its.fit_welcome.Models;

import edu.sdsu.its.fit_welcome.Acutiy;

/**
 * TODO JavaDoc
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
