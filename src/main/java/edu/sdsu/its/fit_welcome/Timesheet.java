package edu.sdsu.its.fit_welcome;

import com.opencsv.CSVWriter;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * Generate Timesheet based on ClockIO {@link Clock.ClockIO} entries.
 *
 * @author Tom Paulus
 *         Created on 12/21/15.
 */
public class Timesheet {
    /**
     * Generate a Timesheet with the total number of hours/minutes worked on a given day.
     * Days where no hours were worked will not be included.
     *
     * @param entries {@link Clock.ClockIO[]} All Clock In/Out entries to be included in the report
     * @param fileName {@link String} Name of the File, Should not include extension
     * @return {@link File} Generated TimeSheet CVS
     */
    public static File make(Clock.ClockIO[] entries, final String fileName) {
        HashMap<String, Double> days = new HashMap<String, Double>(); // Day:Minutes Worked
        for (Clock.ClockIO entry : entries) {
            if (days.containsKey(entry.inTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd")))) {
                days.put(entry.inTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd")), days.get(entry.inTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd"))) + entry.duration.getStandardSeconds() / 60d);
            } else {
                days.put(entry.inTime.toString(DateTimeFormat.forPattern("yyyy-MM-dd")), entry.duration.getStandardSeconds() / 60d);
            }
        }

        File file;
        file = new File(System.getProperty("java.io.tmpdir") + "/" + fileName + ".csv");

        try {
            Logger.getLogger(Timesheet.class).info(String.format("Saving Timesheet into %s.csv", fileName));

            CSVWriter writer = new CSVWriter(new FileWriter(file));
            writer.writeNext(new String[]{"Day", "Hours", "Minutes"}); // Header Row

            for (String day : days.keySet()) {
                final int hours = (int) (days.get(day) / 60);
                final int min = (int) (days.get(day) % 60);

                writer.writeNext(new String[]{day, Integer.toString(hours), Integer.toString(min)});
            }
            writer.close();
        } catch (IOException e) {
            Logger.getLogger(Timesheet.class).error("Problem Saving CSV File", e);
        }

        return file;
    }
}
