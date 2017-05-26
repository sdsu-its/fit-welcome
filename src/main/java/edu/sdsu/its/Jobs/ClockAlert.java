package edu.sdsu.its.Jobs;

import edu.sdsu.its.API.Models.Staff;
import edu.sdsu.its.Welcome.DB;
import edu.sdsu.its.Welcome.SendEmail;
import org.apache.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.text.ParseException;
import java.util.TimeZone;

import static org.quartz.JobBuilder.newJob;

/**
 * @author Tom Paulus
 *         Created on 5/25/17.
 */
public class ClockAlert implements Job {
    public static final String EMAIL_NAME = "Non-Clock Out Notice";
    private static final Logger LOGGER = Logger.getLogger(ClockAlert.class);

    public ClockAlert() {
    }

    /**
     * Schedule the Alert Job to run Every Week night at 10PM Pacific Time
     *
     * @param scheduler {@link Scheduler} Quartz Scheduler Instance
     * @param cron      {@link String} CRON Schedule String for Job
     * @throws SchedulerException Something went wrong scheduling the job
     * @throws ParseException     There was an issue parsing the CRON String
     */
    public static void schedule(Scheduler scheduler, String cron) throws SchedulerException, ParseException {
        JobDetail job = newJob(SyncUserDB.class)
                .withIdentity("SendClockAlerts", "CRON")
                .build();

        // Trigger the job to run now, and then repeat every X Seconds
        CronTriggerImpl trigger = new CronTriggerImpl();
        trigger.setCronExpression(cron);
        trigger.setTimeZone(TimeZone.getTimeZone("US/Pacific"));

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        for (Staff staff : DB.getAllStaff("WHERE clockable = 1")) {
            if (DB.clockStatus(staff.id)) { // User is ClockedIN
                LOGGER.info(String.format("%s %s has not clocked out", staff.firstName, staff.lastName));

                new SendEmail().emailAlert(staff).send(staff.email);
                DB.logEmail(staff.id, EMAIL_NAME);

                LOGGER.info(String.format("Non-Clock Out notice sent to %s %s <%s>", staff.firstName, staff.lastName, staff.email));
            }
        }
    }


}
