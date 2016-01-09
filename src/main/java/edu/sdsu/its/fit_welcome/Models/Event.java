package edu.sdsu.its.fit_welcome.Models;

import edu.sdsu.its.fit_welcome.DB;
import org.joda.time.DateTime;

/**
 * Models a FIT Center Event (When someone comes in to do something)
 *  - Intentionally Vague
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
    public String params;

    public boolean notify;

    public Event(User owner, String type, String params) {
        this.owner = owner;
        this.type = type;
        this.params = params;
    }

    public Event(int id, User owner, DateTime time, String type, String params) {
        this.id = id;
        this.owner = owner;
        this.time = time;
        this.type = type;
        this.params = params;
    }

    public void logEvent() {
        DB.logEvent(owner.id, type, params);
    }
}
