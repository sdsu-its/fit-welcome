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
public class SyncUserDB implements Job {
    private static final int BATCH_SIZE = 100;
    private final Logger LOGGER = Logger.getLogger(this.getClass());

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

        while (!done) {
            int updateCount = 0;


            Users.UserReport userReport = Users.getAllUsers(offset, BATCH_SIZE);
            assert userReport != null;
            assert userReport.users != null;

            LOGGER.info(String.format("Retrieved %d users", userReport.users.length));
            done = userReport.done;

            for (User user : userReport.users) {
                int username;
                if (user.studentId == null || user.studentId.isEmpty()) {
                    LOGGER.warn("StudentID is not defined for User: " + user.externalId);
                    try {
                        username = Integer.parseInt(user.externalId);
                    } catch (NumberFormatException e) {
                        LOGGER.warn(String.format("NumberFormatException - Invalid ID: \"%s\"", user.externalId));
                        continue;
                    }
                } else {
                    try {
                        username = Integer.parseInt(user.studentId);
                    } catch (NumberFormatException e) {
                        LOGGER.warn(String.format("NumberFormatException - Invalid ID: \"%s\"", user.studentId));
                        continue;
                    }
                }

                if (!user.availability.get("available").toUpperCase().equals("YES"))
                    continue;

                DB.syncUser(username,
                        user.name.get("given"),
                        user.name.get("family"),
                        user.contact.get("email"),
                        user.DSK,
                        user.job.get("department"));
                updateCount++;
            }

            LOGGER.warn(String.format("User Sync Completed - Updated %d/%d users", updateCount, BATCH_SIZE));
            offset += BATCH_SIZE;
        }

        LOGGER.warn("Cleaning Users Table");
        DB.cleanUsers(5);
    }
}
