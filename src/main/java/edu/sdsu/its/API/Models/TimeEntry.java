package edu.sdsu.its.API.Models;

import org.apache.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Models a Manual Time Entry, used to add manual clock entries for a use by an admin.
 *
 * @author Tom Paulus
 *         Created on 3/30/16.
 */
public class TimeEntry {
    public User user;
    public String date;
    public boolean direction; // True for Clock In, False for Clock Out

    public String getDate() {
        final String pattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}";

        final Pattern r = Pattern.compile(pattern);
        final Matcher m = r.matcher(date);
        if (!m.find()) {
            Logger.getLogger(this.getClass()).warn("Problem matching time to RegEx Format");
            Logger.getLogger(this.getClass()).debug("Input Date/Time - " +date);
            return null;
        }
        return m.group();
    }
}
