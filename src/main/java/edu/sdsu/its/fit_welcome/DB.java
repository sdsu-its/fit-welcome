package edu.sdsu.its.fit_welcome;

import com.opencsv.CSVWriter;
import edu.sdsu.its.fit_welcome.Models.Event;
import edu.sdsu.its.fit_welcome.Models.Staff;
import edu.sdsu.its.fit_welcome.Models.User;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Interface with the MySQL DB
 *
 * @author Tom Paulus
 *         Created on 12/15/15.
 */
@SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve"})
public class DB {
    private static final Logger Log = Logger.getLogger(DB.class);

    /**
     * Create and return a new DB Connection
     * Don't forget to close the connection!
     *
     * @return {@link Connection} DB Connection
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            final String db_url = Vault.getParam("fit_welcome", "db-url");
            final String db_user = Vault.getParam("fit_welcome", "db-user");
            final String db_pass = Vault.getParam("fit_welcome", "db-password");

            if (db_url != null && db_user != null && db_pass != null) {
                conn = DriverManager.getConnection(
                        db_url,
                        db_user,
                        db_pass);
            }
        } catch (Exception e) {
            Log.fatal("Problem Initializing DB Connection", e);
        }

        return conn;
    }

    private static void executeStatement(final String sql) {
        new Thread() {
            @Override
            public void run() {
                Statement statement = null;
                Connection connection = getConnection();

                try {
                    statement = connection.createStatement();
                    Log.info(String.format("Executing SQL Statement - \"%s\"", sql));
                    statement.execute(sql);

                } catch (SQLException e) {
                    Log.error("Problem Executing Statement \"" + sql + "\"", e);
                } finally {
                    if (statement != null) {
                        try {
                            statement.close();
                            connection.close();
                        } catch (SQLException e) {
                            Log.warn("Problem Closing Statement", e);
                        }
                    }
                }
            }
        }.start();
    }

    private static File queryToCSV(final String sql, final String fileName) throws IOException {
        Connection connection = getConnection();
        Statement statement = null;
        CSVWriter writer = null;
        File file = null;

        try {
            statement = connection.createStatement();
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            file = new File(System.getProperty("java.io.tmpdir") + "/" + fileName + ".csv");

            writer = new CSVWriter(new FileWriter(file));
            writer.writeAll(resultSet, true);

            resultSet.close();

        } catch (SQLException e) {
            Log.error("Problem Querying DB", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.warn("Problem Closing Data Dump File");
                }
            }

            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return file;
    }

    private static String sanitize(final String input) {
        return input.replace("'", "");
    }


    /**
     * Get User for the specified ID
     *
     * @param id {@link int} User's ID (Commonly their RedID)
     * @return {@link User} User
     */
    public static User getUser(final int id) {
        Connection connection = getConnection();
        Statement statement = null;
        User user = null;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT * FROM users WHERE id = " + id + ";";
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                user = new User(id, resultSet.getString("first_name"), resultSet.getString("last_name"), resultSet.getString("email"));
            }

            resultSet.close();
        } catch (SQLException e) {
            Log.error("Problem querying DB for UserID", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return user;
    }

    /**
     * Get User for the specified email
     *
     * @param email {@link String} User's email
     * @return {@link User} User
     */
    public static User getUser(final String email) {
        Connection connection = getConnection();
        Statement statement = null;
        User user = null;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT * FROM users WHERE email = '" + email + "';";
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                user = new User(resultSet.getInt("id"), resultSet.getString("first_name"), resultSet.getString("last_name"), resultSet.getString("email"));
            }

            resultSet.close();
        } catch (SQLException e) {
            Log.error("Problem querying DB for UserID", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return user;
    }

    /**
     * Get Staff User based on ID
     *
     * @param id {@link int} User's ID (Commonly their RedID)
     * @return {@link Staff} Staff
     */
    public static Staff getStaff(final int id) {
        Connection connection = getConnection();
        Statement statement = null;
        Staff staff = null;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT * FROM staff WHERE id = " + id + ";";
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                staff = new Staff(id, resultSet.getString("first_name"), resultSet.getString("last_name"),
                        resultSet.getString("email"), resultSet.getBoolean("clockable"), resultSet.getBoolean("admin"), resultSet.getBoolean("instructional_designer"));
            }


            resultSet.close();
        } catch (SQLException e) {
            Log.error("Problem querying DB for UserID", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return staff;
    }

    /**
     * Get all Staff Users. Restrictions can be imposed with the restrictions param.
     * Use restrictions with care as they are un sanitized and not checked.
     *
     * @param restriction {@link String} Restriction of which users should be included. Uses SQL format
     *                    ex. "WHERE admin = 1"
     *                    Use empty string to get all staff users.
     * @return {@link Staff[]} All staff users who meet the supplied criteria.
     */
    public static Staff[] getAllStaff(final String restriction) {
        Connection connection = getConnection();
        Statement statement = null;
        Staff[] staff = null;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT * FROM staff " + restriction + ";";
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            final List<Staff> staffList = new ArrayList<>();


            while (resultSet.next()) {
                staffList.add(new Staff(resultSet.getInt("id"), resultSet.getString("first_name"), resultSet.getString("last_name"),
                        resultSet.getString("email"), resultSet.getBoolean("clockable"), resultSet.getBoolean("admin"), resultSet.getBoolean("instructional_designer")));
            }

            Collections.sort(staffList, (staff1, staff2) -> staff1.lastName.compareToIgnoreCase(staff2.lastName));


            staff = new Staff[staffList.size()];

            for (int s = 0; s < staffList.size(); s++) {
                staff[s] = staffList.get(s);
            }

            resultSet.close();
        } catch (SQLException e) {
            Log.error("Problem querying DB for Staff List", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return staff;
    }

    /**
     * Log FIT Center Event
     *
     * @param id     {@link int} Users's ID
     * @param action {@link String} User's Goal
     * @param params {@link String} Notes/Specifications for User's visit
     */
    public static void logEvent(final String timestamp, final int id, final String action, String params) {
        params = params != null ? params : ""; // Set Params to an empty string if it is null, which happens when no value is passed in from the API
        final String sql = String.format("INSERT INTO events(TIMESTAMP, redid, action, params) VALUE ('%s', %d, '%s', '%s')", timestamp, id, sanitize(action), sanitize(params));
        executeStatement(sql);
    }

    /**
     * Get the total number of Quotes in the DB.
     * This method assumes that the quotes are have a sequential unique ID!
     *
     * @return {@link int} Number of Quotes in the DB
     */
    public static int getNumQuotes() {
        Connection connection = getConnection();
        Statement statement = null;
        int maxInt = 1;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT MAX(id) FROM quotes;";
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                maxInt = resultSet.getInt(1);
            }

            resultSet.close();
        } catch (SQLException e) {
            Log.error("Problem querying DB for Max Quote ID", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return maxInt;
    }

    /**
     * Returns the Quote with the specified ID
     *
     * @param quoteNum {@link int} Quote ID
     * @return {@link Quote.QuoteModel} Quote Model (Author and Text)
     */
    public static Quote.QuoteModel getQuote(final int quoteNum) {
        Connection connection = getConnection();
        Statement statement = null;
        Quote.QuoteModel quote = null;

        try {
            statement = connection.createStatement();
            final String sql = "SELECT author, text FROM quotes WHERE id = " + quoteNum + ";";
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                quote = new Quote.QuoteModel(resultSet.getString("author"),
                        resultSet.getString("text"));
            }

            resultSet.close();
        } catch (SQLException e) {
            Log.error("Problem querying DB for Quote by ID", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return quote;
    }

    /**
     * Clock In the User
     *
     * @param id   {@link int} User's ID
     * @param time {@link String} Current Time in SQL format
     *             'now()' can be used, but is not recommended because Statements are Threaded.
     */
    public static void clockIn(final int id, final String time) {
        final String sql = String.format("INSERT INTO clock VALUES (%d, %s, DEFAULT );", id, time);
        executeStatement(sql);
    }

    /**
     * Clock Out the User
     *
     * @param id   {@link int} User's ID
     * @param time {@link String} Current Time in SQL format
     *             'now()' can be used, but is not recommended because Statements are Threaded.
     */
    public static void clockOut(final int id, final String time) {
        final String sql = String.format("UPDATE clock SET time_out = %s WHERE id = %d AND time_out = '0000-00-00 00:00:00';\n", time, id);
        executeStatement(sql);
    }

    /**
     * @param id {@link int} Staff ID
     * @return True if Clocked IN, False if Clocked OUT
     */
    public static boolean clockStatus(final int id) {
        Connection connection = getConnection();
        Statement statement = null;
        boolean status = false;

        try {
            statement = connection.createStatement();
            final String sql = String.format("SELECT * FROM clock WHERE id = %d AND time_out = '0000-00-00 00:00:00';", id);
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            status = resultSet.next();

        } catch (SQLException e) {
            Log.error("Problem Adding Action to DB", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return status;
    }

    /**
     * Export all Events to a CSV.
     *
     * @param start    {@link String} Start Date (Inclusive). Use HTML-Date (2015-12-23) format.
     * @param end      {@link String} Start Date (Inclusive). Use HTML-Date (2015-12-23) format.
     * @param fileName {@link String} File Name for the Report. Do NOT include .csv
     * @return {@link File} File object for the Report CSV
     */
    public static File exportEvents(final String start, final String end, final String fileName) {
        final String sql = "SELECT *\n" +
                "FROM events\n" +
                "WHERE\n" +
                "  TIMESTAMP BETWEEN STR_TO_DATE('" + start + "', '%Y-%m-%d') AND\n" +
                "  DATE_ADD(STR_TO_DATE('" + end + "', '%Y-%m-%d'), INTERVAL 1 DAY)\n" +
                "ORDER BY TIMESTAMP ASC;";

        try {
            return queryToCSV(sql, fileName);
        } catch (IOException e) {
            Log.error("Problem Saving Events to CSV", e);
            return null;
        }
    }

    /**
     * Export all ClockIO pairs for a User.
     * - Used to generate Timesheets
     *
     * @param id    {@link int} ID of user whose Clock In/Outs should be queried.
     * @param start {@link String} Start Date (Inclusive). Use HTML-Date (2015-12-23) format.
     * @param end   {@link String} Start Date (Inclusive). Use HTML-Date (2015-12-23) format.
     * @return {@link Clock.ClockIO[]} All ClockIn/Out pairs for the slected user during the specified interval
     */
    public static Clock.ClockIO[] exportClockIOs(final int id, final String start, final String end) {
        final String sql = "SELECT *\n" +
                "FROM clock\n" +
                "WHERE\n" +
                "  id = " + Integer.toString(id) + " AND\n" +
                "  time_in BETWEEN STR_TO_DATE('" + start + "', '%Y-%m-%d') AND\n" +
                "  DATE_ADD(STR_TO_DATE('" + end + "', '%Y-%m-%d'), INTERVAL 1 DAY) AND time_out != '0000-00-00 00:00:00'\n" +
                "ORDER BY time_in ASC;";

        Connection connection = getConnection();
        Statement statement = null;
        Clock.ClockIO[] rarray = null;

        try {
            statement = connection.createStatement();
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            List<Clock.ClockIO> clockIOs = new ArrayList<Clock.ClockIO>();
            while (resultSet.next()) {
                clockIOs.add(new Clock.ClockIO(DateTime.parse(resultSet.getString("time_in"), DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.0")),
                        DateTime.parse(resultSet.getString("time_out"), DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.0"))));
            }

            rarray = new Clock.ClockIO[clockIOs.size()];
            for (int e = 0; e < clockIOs.size(); e++) {
                rarray[e] = clockIOs.get(e);
            }
        } catch (SQLException e) {
            Log.error("Problem retreating Clock entries from DB");
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return rarray;
    }

    /**
     * Create a new Staff User
     *
     * @param staff {@link Staff} New User to Create
     * @return {@link} If user was created successfully. False if the ID already exists.
     */
    public static boolean createNewStaff(final Staff staff) {
        if (getStaff(staff.id) != null) {
            // If a staff member with that record already exists for that ID, thrown an error.
            Log.warn("Cannot create new Staff user, a record with that ID already exists.");
            return false;
        }

        final String sql = String.format("INSERT INTO staff VALUE (%d, '%s', '%s', '%s', %d, %d, %d);",
                staff.id, sanitize(staff.firstName), sanitize(staff.lastName), sanitize(staff.email), staff.clockable ? 1 : 0, staff.admin ? 1 : 0, staff.instructional_designer ? 1 : 0);
        executeStatement(sql);

        return true;
    }

    /**
     * Set the Don't Email flag for the User in the User List
     *
     * @param email {@link String} Email to Unsubscribe
     */
    public static void unsubscribe(final String email) {
        final String sql = "UPDATE users\n" +
                "SET send_emails = 0\n" +
                "WHERE email = '" + email + "';";
        Log.info(String.format("Unsubscribing user with email: %s from FollowUp List", email));
        executeStatement(sql);
    }

    /**
     * Unset the Don't Email flag for the User in the User List
     *
     * @param email {@link String } Email to Resubscribe
     */
    public static void subscribe(final String email) {
        final String sql = "UPDATE users\n" +
                "SET send_emails = 1\n" +
                "WHERE email = '" + email + "';";
        Log.info(String.format("Subscribing user with email: %s to FollowUp List", email));
        executeStatement(sql);
    }

    /**
     * Get the number of Events
     *
     * @return {@link int} Largest event id
     */
    public static int numEvents() {
        final String sql = "SELECT MAX(ID) FROM events;";

        Connection connection = getConnection();
        Statement statement = null;
        int max = 0;

        try {
            statement = connection.createStatement();
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            if (resultSet.next()) {
                max = resultSet.getInt(1);
            }

        } catch (SQLException e) {
            Log.error("Problem retrieving Event entries from DB");
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return max;
    }

    /**
     * Get all Events Today Since X Event ID
     *
     * @param last {@link int} Last eventID fetched - get all since
     * @return {@link List} list of all Events since the provided event
     */
    public static List<Event> getEventsSince(final int last) {
        final String sql = "SELECT * FROM events WHERE ID > " + last + " AND TIMESTAMP >= CURDATE() ORDER BY TIMESTAMP ASC;";

        Connection connection = getConnection();
        Statement statement = null;
        List<Event> events = new ArrayList<>();

        try {
            statement = connection.createStatement();
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                final int redID = resultSet.getInt("redid");
                final User user = User.getUser(redID);
                final Staff staff = user != null ? null : Staff.getStaff(redID);

                events.add(new Event(resultSet.getInt("ID"), user != null ? user : staff, new DateTime(resultSet.getTimestamp("TIMESTAMP")), resultSet.getString("action"), resultSet.getString("params")));
            }

        } catch (SQLException e) {
            Log.error("Problem retrieving Event entries from DB");
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return events;
    }

    /**
     * Get Appointment Matches from the DB
     *
     * @param appointmentType {@link Acutiy.AppointmentType} Appointment Type from Acuity API
     * @return {@link Acutiy.AppointmentType} Appointment Type with Event Params
     */
    public static Acutiy.AppointmentType getAppointmentTypeMatch(Acutiy.AppointmentType appointmentType) {
        final String sql = "SELECT event_text, event_params FROM meetings WHERE acuity_id = " + appointmentType.id + ";";

        Connection connection = getConnection();
        Statement statement = null;

        try {
            statement = connection.createStatement();
            Log.info(String.format("Executing SQL Query - \"%s\"", sql));
            ResultSet resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                appointmentType.eventText = resultSet.getString("event_text");
                appointmentType.eventParams = resultSet.getString("event_params");
            } else {
                appointmentType.eventText = "";
                appointmentType.eventParams = "";
            }
        } catch (SQLException e) {
            Log.error("Problem retrieving Acuity Appointment Matches entries from DB", e);
            appointmentType.eventText = "";
            appointmentType.eventParams = "";
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Problem Closing Statement", e);
                }
            }
        }

        return appointmentType;
    }

    public static void setAppointmentTypeMatch(Acutiy.AppointmentType appointmentType) {
        final String sql = "INSERT INTO meetings (acuity_id, event_text, event_params) VALUES \n" +
                "  (" + appointmentType.id + ", '" + sanitize(appointmentType.eventText) + "', '" + sanitize(appointmentType.eventParams) + "')\n" +
                "ON DUPLICATE KEY UPDATE\n" +
                "  acuity_id    = VALUES(acuity_id),\n" +
                "  event_text   = VALUES(event_text),\n" +
                "  event_params = VALUES(event_params);";
        executeStatement(sql);
    }
}
