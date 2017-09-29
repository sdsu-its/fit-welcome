package edu.sdsu.its.API.Models;

import edu.sdsu.its.Welcome.DB;
import org.joda.time.DateTime;

import java.sql.Timestamp;

/**
 * Models a FIT Center Event (When someone comes in to do something)
 * - Intentionally Vague
 *
 * @author Tom Paulus
 *         Created on 12/20/15.
 */
public class Event {
    public int id;
    public User owner;
    public DateTime time;
    public String timeString;
    public String type;
    public String locale;
    public String params;

    public boolean notify = false;

    public Event(User owner, String type, String params, String locale) {
        this.owner = owner;
        this.type = type;
        this.params = params;
        this.locale = locale;
    }

    public Event(int id, User owner, DateTime time) {
        this.id = id;
        this.owner = owner;
        this.time = time;
    }

    public Event(int id, User owner, DateTime time, String type, String locale, String params) {
        this.id = id;
        this.owner = owner;
        this.time = time;
        this.type = type;
        this.locale = locale;
        this.params = params;
    }

    public Event logEvent() {
        timeString = timeString != null ? timeString : new Timestamp(new java.util.Date().getTime()).toString();
        time = DateTime.now();
        id = DB.logEvent(timeString, owner.id, type, locale, params);
        return this;
    }

    public void completeOwner() {
        if (owner.firstName == null ||
                owner.firstName.isEmpty() ||
                owner.lastName == null ||
                owner.lastName.isEmpty()) {
            // Complete the owner only if necessary
            this.owner = DB.getUser(owner.id);
        }

    }
}
