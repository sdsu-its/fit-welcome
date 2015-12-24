package edu.sdsu.its.fit_welcome.Models;

import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * Models a Clock Cycle (A Clock in / clock out pair)
 *
 * @author Tom Paulus
 *         Created on 12/21/15.
 */
public class ClockIO {
    public DateTime inTime;
    public DateTime outTime;
    public Duration duration;

    public ClockIO(DateTime inTime, DateTime outTime) {
        this.inTime = inTime;
        this.outTime = outTime;
        this.duration = new Duration(inTime, outTime);
    }
}
