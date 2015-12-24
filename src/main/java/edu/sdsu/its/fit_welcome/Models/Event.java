package edu.sdsu.its.fit_welcome.Models;

import edu.sdsu.its.fit_welcome.DB;

/**
 * Models a FIT Center Event (When someone comes in to do something)
 *  - Intentionally Vague
 *
 * @author Tom Paulus
 *         Created on 12/20/15.
 */
public class Event {
    public User owner;
    public String time;
    public String type;
    public String params;

    public Event(User owner, String type, String params) {
        this.owner = owner;
        this.type = type;
        this.params = params;
    }

    public void logEvent() {
        DB.logEvent(owner.id, type, params);
    }
}
