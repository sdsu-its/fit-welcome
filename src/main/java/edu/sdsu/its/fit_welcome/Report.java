package edu.sdsu.its.fit_welcome;

import edu.sdsu.its.fit_welcome.Models.Staff;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Runs Various Reports based on Staff and Visitor activity.
 * Reports are all Runable and designed to be run as an independent thread.
 *
 * @author Tom Paulus
 *         Created on 3/30/16.
 */
public class Report {
    private static final Logger LOGGER = Logger.getLogger(Report.class);

    /**
     * Run a TimeSheet Report
     *
     * Compiles the number of hours/minutes worked by each clockable staff member and emails the result.
     */
    public static class TimesheetReport implements Runnable {
        public final Staff requester;

        public final String startDate;
        public final String endDate;

        public boolean individualReports;

        public TimesheetReport(Staff requester, String startDate, String endDate, boolean individualReports) {
            this.requester = requester;
            this.startDate = startDate;
            this.endDate = endDate;
            this.individualReports = individualReports;
        }

        @Override
        public void run() {
            LOGGER.info("Starting Timesheet Report Thread");

            final Staff[] allClockableStaff = DB.getAllStaff("WHERE clockable = 1");

            if (individualReports) {
                LOGGER.debug("Report Type = individual timesheets");
                for (Staff clockableStaff : allClockableStaff) {
                    File[] timesheets = new File[1];

                    LOGGER.info(String.format("Sending Individual Timesheet to %s %s", clockableStaff.firstName, clockableStaff.lastName));
                    timesheets[0] = Timesheet.make(DB.exportClockIOs(clockableStaff.id, startDate, endDate), "Timesheet");
                    new SendEmail().emailFile("Your Latest Timesheet", clockableStaff.firstName, timesheets).send(clockableStaff.email);
                }
            }
            else{
                LOGGER.debug("Report Type = bulk timesheets (all sent to requester)");
                File[] timesheets = new File[allClockableStaff.length];
                for (int s = 0; s < allClockableStaff.length; s++) {
                    final int staffId = allClockableStaff[s].id;
                    final String staffLast = allClockableStaff[s].lastName;
                    timesheets[s] = Timesheet.make(DB.exportClockIOs(staffId, startDate, endDate), staffLast);
                }

                new SendEmail().emailFile("Staff Timesheets", requester.firstName, timesheets).send(requester.email);
            }
        }
    }

    /**
     * Runs a Usage Report
     *
     * Compiles the usage information (eventID, userID, goal, and any parameters) and emails a CVS files to the requester.
     */
    public static class UsageReport implements Runnable {
        public final Staff requester;

        public final String startDate;
        public final String endDate;

        public UsageReport(Staff requester, String startDate, String endDate) {
            this.requester = requester;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @Override
        public void run() {
            LOGGER.info("Starting Usage Report Thread");
            final File[] eventsCSV = {DB.exportEvents(startDate, endDate, "events")};
            new SendEmail().emailFile("Events Report", requester.firstName, eventsCSV).send(requester.email);
        }
    }
}
