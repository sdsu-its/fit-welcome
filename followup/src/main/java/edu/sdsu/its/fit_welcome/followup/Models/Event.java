package edu.sdsu.its.fit_welcome.followup.Models;


import org.joda.time.DateTime;

/**
 * Models an Event (Check-In)
 *
 * @author Tom Paulus
 *         Created on 1/3/16.
 */
public class Event {
    public int ID;
    public DateTime date;
    public User user;

    public Event(int ID, int redid, DateTime date) {
        this.ID = ID;
        this.user = User.getUser(redid);
        this.date = date;
    }
}
