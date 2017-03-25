package edu.sdsu.its.Jobs;

import edu.sdsu.its.Blackboard.Models.User;
import edu.sdsu.its.Blackboard.Users;
import edu.sdsu.its.fit_welcome.DB;
import org.apache.log4j.Logger;
import org.quartz.*;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Sync the MapDB in the API Endpoint for Courses with the current courses in the Bb API.
 *
 * @author Tom Paulus
 *         Created on 2/3/17.
 */
public class SyncUserDB implements InterruptableJob {
    private static final int BATCH_SIZE = 100;
    private final Logger LOGGER = Logger.getLogger(this.getClass());

    private boolean stopFlag = false;

    public SyncUserDB() {
    }

    /**
     * Schedule the Sync Job
     *
     * @param scheduler
     * @param intervalInHours How often the job should run in Hours
     * @throws SchedulerException
     */
    public static void schedule(Scheduler scheduler, int intervalInHours) throws SchedulerException {
        // define the job and tie it to our MyJob class
        JobDetail job = newJob(SyncUserDB.class)
                .withIdentity("SyncUserListJob", "group1")
                .build();

        // Trigger the job to run now, and then repeat every X Hours
        Trigger trigger = newTrigger()
                .withIdentity("SyncTrigger", "group1")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInHours(intervalInHours)
                        .repeatForever())
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.warn("Starting User Sync");
        boolean done = false;
        int offset = 0;

        while (!done && !stopFlag) {
            int updateCount = 0;

            Users.UserReport userReport = Users.getAllUsers(offset, BATCH_SIZE);
            assert userReport != null;
            assert userReport.users != null;

            LOGGER.info(String.format("Retrieved %d users", userReport.users.length));
            done = userReport.done;

            for (User user : userReport.users) {
                if (!stopFlag) break;
                try {
                    DB.syncUser(getID(user),
                            user.name.get("given"),
                            user.name.get("family"),
                            user.contact.get("email"),
                            user.DSK,
                            user.job != null && user.job.containsKey("department") ? user.job.get("department") : "NULL");
                } catch (Exception e) {
                    LOGGER.warn("Problem Updating User", e);
                }
                updateCount++;
            }

            LOGGER.info(String.format("User Sync Completed - Updated %d/%d users", updateCount, BATCH_SIZE));
            offset += BATCH_SIZE;
        }

        LOGGER.warn("Cleaning Users Table");
        DB.cleanUsers(5);
    }

    private int getID(User user) {
        int username = 0;
        if (user.studentId != null && !user.studentId.isEmpty()) {
            try {
                username = Integer.parseInt(user.externalId);
            } catch (NumberFormatException e) {
                LOGGER.warn(String.format("NumberFormatException - Invalid ID: \"%s\"", user.externalId));
            }
            return username;
        }

        if (user.externalId != null && !user.externalId.isEmpty()) {
            LOGGER.warn("StudentID is not defined for User: " + user.externalId);
            try {
                username = Integer.parseInt(user.externalId);
            } catch (NumberFormatException e) {
                LOGGER.warn(String.format("NumberFormatException - Invalid ID: \"%s\"", user.externalId));
            }
            return username;
        }

        LOGGER.warn("ExternalID is not defined for User: " + user.externalId);
        try {
            username = Integer.parseInt(user.userName);
        } catch (NumberFormatException e) {
            LOGGER.warn(String.format("NumberFormatException - Invalid ID: \"%s\"", user.userName));
        }
        return username;
    }

    /**
     * Stop Job
     */
    @Override
    public void interrupt() throws UnableToInterruptJobException {
        stopFlag = true;
    }
}
