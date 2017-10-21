package edu.sdsu.its.Jobs;

import edu.sdsu.its.API.Models.Event;
import edu.sdsu.its.API.Models.User;
import edu.sdsu.its.Vault;
import edu.sdsu.its.Welcome.DB;
import edu.sdsu.its.Welcome.SendEmail;
import org.apache.log4j.Logger;
import org.joda.time.Duration;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Tom Paulus
 *         Created on 5/25/17.
 */
public class FollowUp implements Job {
    final public static String EMAIL_NAME = "Follow Up Survey";
    private static final Logger LOGGER = Logger.getLogger(FollowUp.class);

    private static List<String> emailsSent = new ArrayList<>();

    public FollowUp() {
    }

    /**
     * Schedule the Follow Up Job to run Every Wednesday and Friday Afternon
     * at 4PM Pacific Time
     *
     * @param scheduler {@link Scheduler} Quartz Scheduler Instance
     * @param cron      {@link String} CRON Schedule String for Job
     * @throws SchedulerException Something went wrong scheduling the job
     * @throws ParseException     There was an issue parsing the CRON String
     */
    public static void schedule(Scheduler scheduler, String cron) throws SchedulerException, ParseException {
        JobDetail job = newJob(FollowUp.class)
                .withIdentity("Follow Up", "CRON")
                .build();

        // Trigger the job to run now, and then repeat every X Seconds
        CronTriggerImpl cronTrigger = new CronTriggerImpl();
        cronTrigger.setCronExpression(cron);
        cronTrigger.setTimeZone(TimeZone.getTimeZone("US/Pacific"));

        Trigger trigger = newTrigger()
                .withIdentity("Follow Up CRON Trigger", "CRON")
                .withSchedule(cronTrigger.getScheduleBuilder())
                .build();

        LOGGER.info(String.format("Scheduling Follow up job with CRON Schedule \"%s\"", cron));
        
        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        final String freshness = Vault.getParam("fit_welcome", "followup_freshness");
        if (freshness == null) {
            LOGGER.error("Followup Freshness not defined in Vault. Aborting!");
            return;
        }
        final Event[] events = DB.exportEvents(Integer.parseInt(freshness));
        for (Event e : events) {
            e.completeOwner();

            if (!emailsSent.contains(e.owner.email)) {
                if (canEmail(e.owner)) {
                    emailsSent.add(e.owner.email);
                    email(e.owner, e);
                } else {
                    LOGGER.info(String.format("Cannot Email %s %s - will be emailed in this run", e.owner.firstName, e.owner.lastName));
                }
            } else {
                LOGGER.info(String.format("Cannot Email %s %s - was recently emailed", e.owner.firstName, e.owner.lastName));
            }
        }
    }

    private static void email(final User user, final Event event) {
        LOGGER.info(String.format("Sending Email to %s for Event: %d", user.email, event.id));

        new SendEmail().emailNotification(user, event).send(user.email);
        DB.logEmail(user.id, "Follow Up Survey");

        LOGGER.info(String.format("Email Sent to %s successfully", user.email));
    }

    private static boolean canEmail(final User user) {
        if (user.subscribed) {
            Duration sinceEmail = DB.lastEmailed(user.id, EMAIL_NAME);
            LOGGER.info(String.format("It has been %s days since %s %s was last Emailed", sinceEmail.getStandardDays(), user.firstName, user.lastName));

            final String followupMax = Vault.getParam("fit_welcome", "followup_max");
            if (followupMax == null) {
                LOGGER.error("Followup Max not defined in Vault. Aborting!");
                return false;
            }
            final Duration maxEmailFreq = new Duration(Integer.parseInt(followupMax) * 86400000);  // 86400000 milliseconds in 1 day
            return sinceEmail.isLongerThan(maxEmailFreq);
        } else {
            LOGGER.info(String.format("%s %s has unsubscribed from all FIT Emails", user.firstName, user.lastName));
            return false;
        }
    }
}
