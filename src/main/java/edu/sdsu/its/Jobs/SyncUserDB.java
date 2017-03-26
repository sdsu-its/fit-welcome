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
 * Sync the Users Table in the DB with the Users from Blackboard via their API.
 * <p>
 * Job is run in relatively small batches, so the frequency needs to be set relatively quick (30-60 seconds);
 * this is done to keep the individual job times down, preventing spikes in memory and CPU usage.
 *
 * @author Tom Paulus
 *         Created on 3/24/17.
 */
public class SyncUserDB implements Job {
    private static final Logger LOGGER = Logger.getLogger(SyncUserDB.class);
    private static final int BATCH_SIZE = 100;

    private static int lastOffset = 0;

    public SyncUserDB() {
    }

    /**
     * Schedule the Sync Job
     *
     * @param scheduler         {@link Scheduler} Quartz Scheduler Instance
     * @param intervalInSeconds How often the job should run in Seconds
     * @throws SchedulerException Something went wrong scheduling the job
     */
    public static void schedule(Scheduler scheduler, int intervalInSeconds) throws SchedulerException {
        // define the job and tie it to our MyJob class
        JobDetail job = newJob(SyncUserDB.class)
                .withIdentity("SyncUserListJob", "group1")
                .build();

        // Trigger the job to run now, and then repeat every X Hours
        Trigger trigger = newTrigger()
                .withIdentity("SyncTrigger", "group1")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(intervalInSeconds)
                        .repeatForever())
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.debug("Starting User Sync");

        int offset = lastOffset;
        int updateCount = 0;
        boolean done;

        Users.UserReport userReport = Users.getAllUsers(offset, BATCH_SIZE);
        if (userReport == null || userReport.users == null || userReport.users.length == 0){
            LOGGER.info("Received Empty Payload from API - Stopping!");
            return;
        } else {
            LOGGER.debug(String.format("Retrieved %d users", userReport.users.length));
            done = userReport.done;

            for (User user : userReport.users) {
                final int id = getID(user);
                if (id == 0) continue;

                if (user.availability == null || !user.availability.get("available").equals("Yes")) {
                    LOGGER.info(String.format("User %d is not available - Skipping", id));
                    continue;
                }

                try {
                    LOGGER.debug(String.format("Syncing User %d - %s", id, user.toString()));

                    DB.syncUser(id,
                            user.name.get("given"),
                            user.name.get("family"),
                            user.contact.get("email"),
                            user.DSK,
                            user.job != null && user.job.containsKey("department") ? user.job.get("department") : "NULL");
                    updateCount++;
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // Intentionally Blank
                } catch (NoClassDefFoundError | IllegalStateException e) {
                    // Abort the Thread, quickly and cleanly
                    return;
                } catch (Exception e) {
                    LOGGER.warn("Problem Updating User", e);
                }
            }

            LOGGER.info(String.format("User Sync Completed - Updated %d/%d users", updateCount, BATCH_SIZE));
            lastOffset += BATCH_SIZE;
        }

        if (done) doneProcedure();
    }

    private void doneProcedure() {
        LOGGER.info("Completed User Sync - Resetting offset");
        lastOffset = 0;

        LOGGER.info("Cleaning Users Table");
        DB.cleanUsers(5);
    }

    private int getID(User user) {
        int username = 0;
        if (user.studentId != null && !user.studentId.isEmpty()) {
            try {
                username = Integer.parseInt(user.externalId);
                return username;
            } catch (NumberFormatException e) {
                LOGGER.warn(String.format("NumberFormatException - Invalid ID: \"%s\"", user.externalId));
            }
        }

        if (user.externalId != null && !user.externalId.isEmpty()) {
            LOGGER.debug("StudentID is not defined for User: " + user.externalId);
            try {
                username = Integer.parseInt(user.externalId);
                return username;
            } catch (NumberFormatException e) {
                LOGGER.warn(String.format("NumberFormatException - Invalid ID: \"%s\"", user.externalId));
            }
        }

        LOGGER.debug("ExternalID is not defined for User: " + user.externalId);
        try {
            username = Integer.parseInt(user.userName);
        } catch (NumberFormatException e) {
            LOGGER.warn(String.format("NumberFormatException - Invalid ID: \"%s\"", user.userName));
        }

        return username;
    }
}
