package edu.sdsu.its.Welcome;

import com.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generate Timesheet based on ClockIO {@link Clock.ClockIO} entries.
 *
 * @author Tom Paulus
 * Created on 12/21/15.
 */
public class Timesheet {
    private static final String[] DAYS_OF_THE_WEEK = {"Su", "M", "T", "W", "Th", "F", "Sa"};

    /**
     * Generate a Timesheet with the total number of hours/minutes worked on a given day.
     * Days where no hours were worked will not be included.
     *
     * @param entries  {@link Clock.ClockIO[]} All Clock In/Out entries to be included in the report
     * @param fileName {@link String} Name of the File, Should not include extension
     * @return {@link File} Generated TimeSheet CVS
     */
    public static File make(Clock.ClockIO[] entries, final String fileName) {
        Map<DateTime, Double> days = new TreeMap<>(); // Day:Minutes Worked
        for (Clock.ClockIO entry : entries) {
            if (entry.inTime == null || entry.outTime == null) {
                continue;
            }

            final DateTime inTime = entry.inTime;
            if (days.containsKey(inTime)) {
                days.put(inTime, days.get(inTime) + entry.duration.getStandardSeconds() / 60d);
            } else {
                days.put(inTime, entry.duration.getStandardSeconds() / 60d);
            }
        }

        File file;
        file = new File(System.getProperty("java.io.tmpdir") + "/" + fileName + ".csv");

        try {
            Logger.getLogger(Timesheet.class).info(String.format("Saving Timesheet into %s.csv", fileName));

            CSVWriter writer = new CSVWriter(new FileWriter(file));
            writer.writeNext(new String[]{"Day", "Day of Week", "Hours", "Minutes", "Hours Fractional"}); // Header Row

            for (DateTime day : days.keySet()) {
                final String date = day.toString(DateTimeFormat.forPattern("yyyy-MM-dd"));
                final String dow = DAYS_OF_THE_WEEK[day.dayOfWeek().get()];
                final String hours = Integer.toString((int) (days.get(day) / 60));
                final String min = Integer.toString((int) (days.get(day) % 60));
                final String hours_fractional = String.format("%.1f", days.get(day) / 60 + 0.05);
                // .05 is added to ensure that the number of hours is always rounded up.

                writer.writeNext(new String[]{date, dow, hours, min, hours_fractional});
            }
            writer.close();
        } catch (IOException e) {
            Logger.getLogger(Timesheet.class).error("Problem Saving CSV File", e);
        }

        return file;
    }
}
