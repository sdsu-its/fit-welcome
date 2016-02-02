package edu.sdsu.its.fit_welcome.alerts;

import edu.sdsu.its.fit_welcome.alerts.Models.Staff;
import org.apache.log4j.Logger;

/**
 * Main Executable for Alerts
 *
 * @author Tom Paulus
 *         Created on 2/1/16.
 */
public class Main {
    public static final String EMAIL_NAME = "Non-Clock Out Notice";
    private static final Logger Log = Logger.getLogger(DB.class);


    public static void main(String[] args) {
        for (Staff staff : DB.getAllStaff("WHERE clockable = 1")) {
            if (DB.clockStatus(staff.id)) { // User is ClockedIN
                Log.info(String.format("%s %s has not clocked out", staff.firstName, staff.lastName));

                new SendEmail().emailNotification(staff).send(staff.email);
                DB.logEmail(staff.id, EMAIL_NAME);

                Log.info(String.format("Non-Clock Out notice sent to %s %s <%s>", staff.firstName, staff.lastName, staff.email));
            }
        }
    }
}
