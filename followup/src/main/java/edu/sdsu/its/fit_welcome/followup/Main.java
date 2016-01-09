package edu.sdsu.its.fit_welcome.followup;

import edu.sdsu.its.fit_welcome.followup.Models.Event;
import edu.sdsu.its.fit_welcome.followup.Models.User;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;


/**
 * Main Executable for FollowUp
 *
 * @author Tom Paulus
 *         Created on 1/3/16.
 */
public class Main {
    final public static String Email_Name = "Follow Up Survey";
    private static Logger Log = Logger.getLogger(Main.class);

    private static List<String> emailsSent = new ArrayList<>();

    private static void email(final User user, final Event event) {
        Log.info(String.format("Sending Email to %s for Event: %d", user.email, event.ID));

        new SendEmail().emailNotification(user, event).send(user.email);
        DB.logEmail(user.id, "Follow Up Survey");

        Log.info(String.format("Email Sent to %s successfully", user.email));
    }

    private static boolean canEmail(final User user) {
        if (user.subscribed) {
            Duration sinceEmail = DB.lastEmailed(user.id);
            Log.info(String.format("It has been %s days since %s %s was last Emailed", sinceEmail.getStandardDays(), user.firstName, user.lastName));

            final Duration maxEmailFreq = new Duration(Integer.parseInt(Param.getParam("fit_welcome", "followup_max")) * 86400000);  // 86400000 milliseconds in 1 day
            return sinceEmail.isLongerThan(maxEmailFreq);
        } else {
            Log.info(String.format("%s %s has unsubscribed from all FIT Emails", user.firstName, user.lastName));
            return false;
        }
    }

    public static void main(String[] args) {
        final Event[] events = DB.exportEvents(Integer.parseInt(Param.getParam("fit_welcome", "followup_freshness")));
        for (Event e : events) {
            if (!emailsSent.contains(e.user.email)) {
                if (canEmail(e.user)) {
                    emailsSent.add(e.user.email);

                    new Thread() {
                        @Override
                        public void run() {
                            Log.debug(String.format("Starting new Thread for Event: %d - %s %s", e.ID, e.user.firstName, e.user.lastName));
                            email(e.user, e);
                        }
                    }.start();
                } else {
                    Log.info(String.format("Cannot Email %s %s - will be emailed in this run", e.user.firstName, e.user.lastName));
                }
            } else {
                Log.info(String.format("Cannot Email %s %s - was recently emailed", e.user.firstName, e.user.lastName));
            }


        }
    }
}
