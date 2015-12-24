package edu.sdsu.its.fit_welcome;

import edu.sdsu.its.fit_welcome.Models.User;

import java.sql.Timestamp;
import java.util.Date;

/**
 * TODO JavaDoc
 *
 * @author Tom Paulus
 *         Created on 12/12/15.
 */
public class Clock {
    final User user;

    public Clock(final User user) {
        this.user = user;
    }

    /**
     * Get current Clock In/Out status
     *
     * @return True if Clocked IN, False if Clocked OUT
     */
    public boolean getStatus() {
        return DB.clockStatus(this.user.id);
    }

    /**
     * Toggle the Clock Status.
     * If they are currently clocked in, clock them out and vise-a-versa.
     *
     * @return {@link boolean} Current clock status after toggling the clock (True if Clocked IN, False if Clocked OUT)
     */
    public boolean toggle() {
        if (getStatus()) {
            // Currently Clocked IN, needs to be Clocked OUT
            DB.clockOut(this.user.id, String.format("'%s'", new Timestamp(new Date().getTime()).toString()));
            return false;

        } else {
            // Currently Clocked IN, needs to be Clocked OUT
            DB.clockIn(this.user.id, String.format("'%s'", new Timestamp(new Date().getTime()).toString()));
            return true;
        }
    }
}
