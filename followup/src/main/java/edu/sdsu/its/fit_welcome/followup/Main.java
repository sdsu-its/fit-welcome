package edu.sdsu.its.fit_welcome.followup;

import edu.sdsu.its.fit_welcome.followup.Models.Event;
import edu.sdsu.its.fit_welcome.followup.Models.User;
import org.apache.log4j.Logger;
import org.joda.time.Duration;


/**
 * Main Executable for FollowUp
 *
 * @author Tom Paulus
 *         Created on 1/3/16.
 */
public class Main {
    private static Logger Log = Logger.getLogger(Main.class);
    final public static String Email_Name = "Follow Up Survey";

    private static void email(final User user, final Event event) {
        Log.info(String.format("Sending Email to %s for Event: %d", user.email, event.ID));

        new SendEmail().emailNotification(user, event).send(user.email);
        DB.logEmail(user.id, "Follow Up Survey");

        Log.info(String.format("Email Sent to %s successfully", user.email));
    }

    private static boolean canEmail(final User user) {
        if (user.subscribed) {
            Duration sinceEmail = DB.lastEmailed(user.id);
            Log.debug(String.format("It has been %s days since %s %s was last Emailed", sinceEmail.getStandardDays(), user.firstName, user.lastName));

            final Duration maxEmailFreq = new Duration(Integer.parseInt(Param.getParam("fit_welcome", "followup_max")) * 86400000);  // 86400000 milliseconds in 1 day
            return sinceEmail.isLongerThan(maxEmailFreq);
        }
        else {
            Log.debug(String.format("%s %s has unsubscribed from all FIT Emails", user.firstName, user.lastName));
            return false;
        }
    }

    public static void main(String[] args) {
        for (Event e : DB.exportEvents(Integer.parseInt(Param.getParam("fit_welcome", "followup_freshness")))) {
            new Thread() {
                @Override
                public void run() {
                    Log.debug(String.format("Starting new Thread for Event: %d - %s %s", e.ID, e.user.firstName, e.user.lastName));
                    if (canEmail(e.user)) {
                        email(e.user, e);
                    } else {
                        Log.info(String.format("Cannot Email %s %s - was recently emailed", e.user.firstName, e.user.lastName));
                    }
                }
            }.start();
        }
    }
}
